package com.mouse.mes.knowledge.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/** Task-two document-scoped eager indexing, exact-vector reuse and original authorized objects. */
@Service
public class AuthorizedChunkIndex {
    private final JdbcTemplate db;
    private final RagModelClient model;
    private final ObjectMapper json;
    private final Map<Long,ReentrantLock> locks=new ConcurrentHashMap<>();
    @Value("${MES_RAG_MIN_SCORE:0.5}") private double minimumScore=0.5;
    public AuthorizedChunkIndex(JdbcTemplate db,RagModelClient model,ObjectMapper json,RagSchema schema){
        this.db=db;this.model=model;this.json=json;
        db.execute("CREATE TABLE IF NOT EXISTS rag_authorized_vector(owner BIGINT NOT NULL,chunk_id VARCHAR(96) NOT NULL,document_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT '' NOT NULL,version BIGINT NOT NULL DEFAULT 0,fingerprint CHAR(64) NOT NULL,embedding_model VARCHAR(200) NOT NULL,embedding JSON NOT NULL,PRIMARY KEY(owner,chunk_id)) ENGINE=InnoDB");
        for(String column:List.of("document_id","version")){
            try(var connection=Objects.requireNonNull(db.getDataSource()).getConnection();var columns=connection.getMetaData().getColumns(connection.getCatalog(),null,"rag_authorized_vector",column)){
                if(!columns.next())db.execute("ALTER TABLE rag_authorized_vector ADD COLUMN "+column+(column.equals("version")?" BIGINT NOT NULL DEFAULT 0":" VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT '' NOT NULL"));
            }catch(java.sql.SQLException e){throw new IllegalStateException("向量索引表升级失败",e);}
        }
    }
    public String name(){return "向量检索 · "+model.embeddingModel()+" · MySQL余弦精确召回";}
    private ReentrantLock lock(long owner){return locks.computeIfAbsent(owner,k->new ReentrantLock());}
    public void evict(long owner,List<String> ids){var lock=lock(owner);lock.lock();try{for(String id:ids)db.update("DELETE FROM rag_authorized_vector WHERE owner=? AND chunk_id=?",owner,id);}finally{lock.unlock();}}
    public void deleteDocument(long owner,String id,long version){deleteDocument(owner,id,version,List.of());}
    public void deleteDocument(long owner,String id,long version,List<String> legacyIds){
        RagFileService.validateId(id);var lock=lock(owner);lock.lock();try{
            db.update("DELETE FROM rag_authorized_vector WHERE owner=? AND document_id=? AND version<=?",owner,id,version);
            for(String chunk:legacyIds)db.update("DELETE FROM rag_authorized_vector WHERE owner=? AND chunk_id=? AND document_id=''",owner,chunk);
        }finally{lock.unlock();}
    }
    public String location(long owner,String chunk,String fallback){
        var rows=db.queryForList("SELECT v.location FROM rag_vector v JOIN rag_document d ON d.owner=v.owner AND d.document_id=v.document_id AND d.active_version=v.version WHERE v.owner=? AND v.chunk_id=? AND d.deleted=FALSE",owner,chunk);
        return rows.isEmpty()?fallback:rows.get(0).get("location").toString();
    }
    public <T> void prepareDocument(long owner,List<T> chunks,Function<T,RagTypes.Chunk> mapper)throws Exception{prepareDocument(owner,chunks,mapper,List.of());}
    public <T> void prepareDocument(long owner,List<T> chunks,Function<T,RagTypes.Chunk> mapper,List<String> previous)throws Exception{
        var lock=lock(owner);lock.lock();try{synchronize(owner,chunks,mapper,true,previous);}finally{lock.unlock();}
    }
    public <T> List<T> retrieve(long owner,String question,List<T> authorized,int limit,Function<T,RagTypes.Chunk> mapper)throws Exception{
        if(owner<1||limit<1||limit>50||authorized.size()>2000||question==null||question.isBlank()||question.length()>1200)throw new IllegalArgumentException("检索参数超过限制");
        var lock=lock(owner);lock.lock();try{
            var stored=synchronize(owner,authorized,mapper,false,List.of());if(authorized.isEmpty())return List.of();
            double[] query=model.embed(owner,List.of(question)).get(0);List<Map.Entry<T,Double>> scored=new ArrayList<>();
            for(T value:authorized)scored.add(Map.entry(value,RagVectorService.cosine(query,stored.get(mapper.apply(value).id()))));
            scored.sort(Map.Entry.<T,Double>comparingByValue().reversed());return scored.stream().filter(e->e.getValue()>=minimumScore).limit(limit).map(Map.Entry::getKey).toList();
        }finally{lock.unlock();}
    }
    private <T> Map<String,double[]> synchronize(long owner,List<T> authorized,Function<T,RagTypes.Chunk> mapper,boolean publication,List<String> previous)throws Exception{
        if(owner<1||authorized.size()>2000)throw new IllegalArgumentException("授权片段超过限制");
        Map<String,RagTypes.Chunk> chunks=new LinkedHashMap<>();Map<String,String> fingerprints=new HashMap<>();Set<String> documents=new HashSet<>();
        for(T value:authorized){var c=mapper.apply(value);RagFileService.validateId(c.documentId());
            if(c.version()<1||c.id()==null||c.id().length()>96||c.text().isBlank()||c.text().length()>2000||chunks.put(c.id(),c)!=null)throw new IllegalArgumentException("授权片段格式无效或编号重复");
            fingerprints.put(c.id(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(c))));documents.add(c.documentId());
        }
        Map<String,double[]> stored=new HashMap<>();
        for(var row:db.queryForList("SELECT * FROM rag_authorized_vector WHERE owner=?",owner)){
            String id=row.get("chunk_id").toString();var c=chunks.get(id);
            boolean replace=documents.contains(row.get("document_id"))||(publication&&previous.contains(id));
            if(c==null){if(replace)db.update("DELETE FROM rag_authorized_vector WHERE owner=? AND chunk_id=?",owner,id);continue;}
            if(!fingerprints.get(id).equals(row.get("fingerprint"))||!model.embeddingModel().equals(row.get("embedding_model")))db.update("DELETE FROM rag_authorized_vector WHERE owner=? AND chunk_id=?",owner,id);
            else{stored.put(id,json.readValue(row.get("embedding").toString(),double[].class));db.update("UPDATE rag_authorized_vector SET document_id=?,version=? WHERE owner=? AND chunk_id=?",c.documentId(),c.version(),owner,id);}
        }
        for(var c:chunks.values())if(!stored.containsKey(c.id())){
            var rows=db.queryForList("SELECT v.content,v.embedding FROM rag_vector v JOIN rag_document d ON d.owner=v.owner AND d.document_id=v.document_id AND d.active_version=v.version WHERE v.owner=? AND v.document_id=? AND v.version=? AND v.embedding_model=? AND d.deleted=FALSE",owner,c.documentId(),c.version(),model.embeddingModel());
            for(var row:rows)if(c.text().equals(row.get("content"))){double[] vector=json.readValue(row.get("embedding").toString(),double[].class);save(owner,c,fingerprints.get(c.id()),vector);stored.put(c.id(),vector);break;}
        }
        var missing=chunks.keySet().stream().filter(id->!stored.containsKey(id)).toList();
        for(int start=0;start<missing.size();start+=8){var batch=missing.subList(start,Math.min(start+8,missing.size()));var vectors=model.embed(owner,batch.stream().map(id->chunks.get(id).text()).toList());
            if(vectors.size()!=batch.size())throw new IllegalStateException("向量数量与片段不一致");
            for(int i=0;i<batch.size();i++){String id=batch.get(i);save(owner,chunks.get(id),fingerprints.get(id),vectors.get(i));stored.put(id,vectors.get(i));}
        }
        return stored;
    }
    private void save(long owner,RagTypes.Chunk c,String fingerprint,double[] vector)throws Exception{
        db.update("INSERT INTO rag_authorized_vector(owner,chunk_id,document_id,version,fingerprint,embedding_model,embedding) VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE document_id=VALUES(document_id),version=VALUES(version),fingerprint=VALUES(fingerprint),embedding_model=VALUES(embedding_model),embedding=VALUES(embedding)",owner,c.id(),c.documentId(),c.version(),fingerprint,model.embeddingModel(),json.writeValueAsString(vector));
    }
}
