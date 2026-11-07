package com.mouse.mes.knowledge.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/** Task-three bridge: indexes only its already-authorized published chunks and returns original objects. */
@Service
public class AuthorizedChunkIndex {
    private final JdbcTemplate db;
    private final RagModelClient model;
    private final ObjectMapper json;
    private final Map<Long,ReentrantLock> locks=new ConcurrentHashMap<>();
    public AuthorizedChunkIndex(JdbcTemplate db,RagModelClient model,ObjectMapper json,RagSchema schema) {
        this.db=db;this.model=model;this.json=json;
        db.execute("CREATE TABLE IF NOT EXISTS rag_authorized_vector (owner BIGINT NOT NULL, chunk_id VARCHAR(96) NOT NULL, document_id VARCHAR(64) NOT NULL DEFAULT '', version BIGINT NOT NULL DEFAULT 0, fingerprint CHAR(64) NOT NULL, embedding_model VARCHAR(200) NOT NULL, embedding JSON NOT NULL, PRIMARY KEY(owner,chunk_id)) ENGINE=InnoDB");
        for(String column:List.of("document_id","version")) {
            try(var connection=Objects.requireNonNull(db.getDataSource()).getConnection();
                var columns=connection.getMetaData().getColumns(connection.getCatalog(),null,"rag_authorized_vector",column)) {
                if(!columns.next()) db.execute("ALTER TABLE rag_authorized_vector ADD COLUMN "+column+(column.equals("version")?" BIGINT NOT NULL DEFAULT 0":" VARCHAR(64) NOT NULL DEFAULT ''"));
            }catch(java.sql.SQLException e){throw new IllegalStateException("向量索引表升级失败",e);}
        }
    }
    public String name(){return "向量检索 · "+model.embeddingModel()+" · MySQL余弦精确召回";}
    public <T> void prepareDocument(long owner,List<T> chunks,Function<T,RagTypes.Chunk> mapper) throws Exception {
        synchronize(owner,chunks,mapper,false);
    }
    public void deleteDocument(long owner,String id,long version) {
        db.update("DELETE FROM rag_authorized_vector WHERE owner=? AND document_id=? AND version<=?",owner,id,version);
    }
    public String location(long owner,String chunkId,String fallback){
        var rows=db.queryForList("SELECT v.location FROM rag_vector v JOIN rag_document d ON v.owner=d.owner AND v.document_id=d.document_id AND v.version=d.active_version WHERE v.owner=? AND v.chunk_id=? AND d.deleted=FALSE",owner,chunkId);
        return rows.isEmpty()?fallback:rows.get(0).get("location").toString();
    }
    public <T> List<T> retrieve(long owner,String question,List<T> authorized,int limit,Function<T,RagTypes.Chunk> mapper) throws Exception {
        if(owner<1||limit<1||limit>50||authorized.size()>2000||question==null||question.isBlank()||question.length()>1200)
            throw new IllegalArgumentException("检索参数超过限制");
        ReentrantLock lock=locks.computeIfAbsent(owner,k->new ReentrantLock());lock.lock();
        try {
            Map<String,T> originals=new LinkedHashMap<>();
            authorized.forEach(c->originals.put(mapper.apply(c).id(),c));
            Map<String,double[]> stored=synchronize(owner,authorized,mapper,authorized.isEmpty());
            if(originals.isEmpty()) return List.of();
            double[] query=model.embed(owner,List.of(question)).get(0);
            List<Map.Entry<String,Double>> scored=new ArrayList<>();
            for(String id:originals.keySet()) scored.add(Map.entry(id,RagVectorService.cosine(query,stored.get(id))));
            scored.sort(Map.Entry.<String,Double>comparingByValue().reversed());
            return scored.stream().filter(e->e.getValue()>=0.5).limit(limit).map(entry->originals.get(entry.getKey())).toList();
        } finally {lock.unlock();}
    }
    private <T> Map<String,double[]> synchronize(long owner,List<T> authorized,Function<T,RagTypes.Chunk> mapper,boolean fullScope) throws Exception {
            Map<String,T> originals=new LinkedHashMap<>();Map<String,String> fingerprints=new HashMap<>();
            Set<String> documents=new HashSet<>();
            for(T value:authorized) {
                var c=mapper.apply(value);RagFileService.validateId(c.documentId());
                if(c.version()<1||c.id()==null||c.id().length()>96||c.text().isBlank()||c.text().length()>2000||originals.put(c.id(),value)!=null)
                    throw new IllegalArgumentException("授权片段格式无效或编号重复");
                fingerprints.put(c.id(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(c))));
                documents.add(c.documentId());
            }
            Map<String,double[]> stored=new HashMap<>();
            for(var row:db.queryForList("SELECT * FROM rag_authorized_vector WHERE owner=?",owner)) {
                String id=row.get("chunk_id").toString();
                if(!fullScope&&!documents.contains(row.get("document_id"))) continue;
                if(!Objects.equals(fingerprints.get(id),row.get("fingerprint"))||!model.embeddingModel().equals(row.get("embedding_model"))) {
                    db.update("DELETE FROM rag_authorized_vector WHERE owner=? AND chunk_id=?",owner,id);
                } else stored.put(id,json.readValue(row.get("embedding").toString(),double[].class));
            }
            if(originals.isEmpty()) return stored;
            List<String> missing=originals.keySet().stream().filter(id->!stored.containsKey(id)).toList();
            for(int start=0;start<missing.size();start+=8) {
                var batch=missing.subList(start,Math.min(start+8,missing.size()));
                var vectors=model.embed(owner,batch.stream().map(id->mapper.apply(originals.get(id)).text()).toList());
                for(int i=0;i<batch.size();i++) {
                    String id=batch.get(i);double[] vector=vectors.get(i);
                    var chunk=mapper.apply(originals.get(id));
                    db.update("INSERT INTO rag_authorized_vector(owner,chunk_id,document_id,version,fingerprint,embedding_model,embedding) VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE document_id=VALUES(document_id),version=VALUES(version),fingerprint=VALUES(fingerprint),embedding_model=VALUES(embedding_model),embedding=VALUES(embedding)",owner,id,chunk.documentId(),chunk.version(),fingerprints.get(id),model.embeddingModel(),json.writeValueAsString(vector));
                    stored.put(id,vector);
                }
            }
            return stored;
    }
}
