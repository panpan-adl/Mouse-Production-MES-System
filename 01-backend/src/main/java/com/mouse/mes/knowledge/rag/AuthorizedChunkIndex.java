package com.mouse.mes.knowledge.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${MES_RAG_MIN_SCORE:0.5}") private double minimumScore=0.5;
    public AuthorizedChunkIndex(JdbcTemplate db,RagModelClient model,ObjectMapper json,RagSchema schema) {
        this.db=db;this.model=model;this.json=json;
        db.execute("CREATE TABLE IF NOT EXISTS rag_authorized_vector (owner BIGINT NOT NULL, chunk_id VARCHAR(96) NOT NULL, fingerprint CHAR(64) NOT NULL, embedding_model VARCHAR(200) NOT NULL, embedding JSON NOT NULL, PRIMARY KEY(owner,chunk_id)) ENGINE=InnoDB");
    }
    public String name(){return "向量检索 · "+model.embeddingModel()+" · MySQL余弦精确召回";}
    public void evict(long owner,List<String> ids){
        ReentrantLock lock=locks.computeIfAbsent(owner,k->new ReentrantLock());lock.lock();
        try{for(String id:ids)db.update("DELETE FROM rag_authorized_vector WHERE owner=? AND chunk_id=?",owner,id);}finally{lock.unlock();}
    }
    public <T> List<T> retrieve(long owner,String question,List<T> authorized,int limit,Function<T,RagTypes.Chunk> mapper) throws Exception {
        if(owner<1||limit<1||limit>50||authorized.size()>2000||question==null||question.isBlank()||question.length()>1200)
            throw new IllegalArgumentException("检索参数超过限制");
        ReentrantLock lock=locks.computeIfAbsent(owner,k->new ReentrantLock());lock.lock();
        try {
            Map<String,T> originals=new LinkedHashMap<>();Map<String,String> fingerprints=new HashMap<>();
            for(T value:authorized) {
                var c=mapper.apply(value);RagFileService.validateId(c.documentId());
                if(c.version()<1||c.id()==null||c.id().length()>96||c.text().isBlank()||c.text().length()>2000||originals.put(c.id(),value)!=null)
                    throw new IllegalArgumentException("授权片段格式无效或编号重复");
                fingerprints.put(c.id(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(c))));
            }
            Map<String,double[]> stored=new HashMap<>();
            for(var row:db.queryForList("SELECT * FROM rag_authorized_vector WHERE owner=?",owner)) {
                String id=row.get("chunk_id").toString();
                if(!Objects.equals(fingerprints.get(id),row.get("fingerprint"))||!model.embeddingModel().equals(row.get("embedding_model"))) {
                    db.update("DELETE FROM rag_authorized_vector WHERE owner=? AND chunk_id=?",owner,id);
                } else stored.put(id,json.readValue(row.get("embedding").toString(),double[].class));
            }
            if(originals.isEmpty()) return List.of();
            // Reuse task two's already-published embedding when the exact authorized text/model matches.
            // Only the identifier changes to task three's shared buildId:ordinal; no extra cloud call.
            for(String id:originals.keySet())if(!stored.containsKey(id)) {
                var c=mapper.apply(originals.get(id));
                var rows=db.queryForList("SELECT v.content,v.embedding FROM rag_vector v JOIN rag_document d ON d.owner=v.owner AND d.document_id=v.document_id AND d.active_version=v.version WHERE v.owner=? AND v.document_id=? AND v.version=? AND v.embedding_model=? AND d.deleted=FALSE",owner,c.documentId(),c.version(),model.embeddingModel());
                for(var row:rows)if(c.text().equals(row.get("content"))) {
                    double[] vector=json.readValue(row.get("embedding").toString(),double[].class);
                    db.update("INSERT INTO rag_authorized_vector(owner,chunk_id,fingerprint,embedding_model,embedding) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE fingerprint=VALUES(fingerprint),embedding_model=VALUES(embedding_model),embedding=VALUES(embedding)",owner,id,fingerprints.get(id),model.embeddingModel(),json.writeValueAsString(vector));
                    stored.put(id,vector);break;
                }
            }
            List<String> missing=originals.keySet().stream().filter(id->!stored.containsKey(id)).toList();
            for(int start=0;start<missing.size();start+=8) {
                var batch=missing.subList(start,Math.min(start+8,missing.size()));
                var vectors=model.embed(owner,batch.stream().map(id->mapper.apply(originals.get(id)).text()).toList());
                for(int i=0;i<batch.size();i++) {
                    String id=batch.get(i);double[] vector=vectors.get(i);
                    db.update("INSERT INTO rag_authorized_vector(owner,chunk_id,fingerprint,embedding_model,embedding) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE fingerprint=VALUES(fingerprint),embedding_model=VALUES(embedding_model),embedding=VALUES(embedding)",owner,id,fingerprints.get(id),model.embeddingModel(),json.writeValueAsString(vector));
                    stored.put(id,vector);
                }
            }
            double[] query=model.embed(owner,List.of(question)).get(0);
            List<Map.Entry<String,Double>> scored=new ArrayList<>();
            for(String id:originals.keySet()) scored.add(Map.entry(id,RagVectorService.cosine(query,stored.get(id))));
            scored.sort(Map.Entry.<String,Double>comparingByValue().reversed());
            return scored.stream().filter(entry->entry.getValue()>=minimumScore).limit(limit).map(entry->originals.get(entry.getKey())).toList();
        } finally {lock.unlock();}
    }
}
