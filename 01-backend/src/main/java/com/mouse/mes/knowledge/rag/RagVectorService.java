package com.mouse.mes.knowledge.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/** Small private libraries use exact cosine search over durable local dense vectors. */
@Service
public class RagVectorService {
    private final JdbcTemplate db;
    private final RagModelClient models;
    private final ObjectMapper json;
    @Value("${MES_RAG_MIN_SCORE:0.5}") private double minimumScore;
    public RagVectorService(JdbcTemplate db, RagModelClient models, ObjectMapper json) {
        this.db=db; this.models=models; this.json=json;
    }
    public static double cosine(double[] a,double[] b) {
        if(a.length!=b.length) throw new IllegalStateException("索引维度与当前模型不一致，请更新文件重建索引");
        double dot=0,aa=0,bb=0;
        for(int i=0;i<a.length;i++){dot+=a[i]*b[i];aa+=a[i]*a[i];bb+=b[i]*b[i];}
        return aa==0||bb==0 ? 0 : dot/Math.sqrt(aa*bb);
    }
    private List<Map<String,Object>> rows(long owner) {
        // The database filters the owner and published version BEFORE any scoring or Top-K.
        return db.queryForList("SELECT v.* FROM rag_vector v JOIN rag_document d ON v.owner=d.owner AND v.document_id=d.document_id AND v.version=d.active_version WHERE v.owner=? AND d.deleted=FALSE",owner);
    }
    public RagTypes.Answer ask(long owner,String question,String mode) throws Exception {
        if(question==null||question.isBlank()||question.length()>1200) throw new IllegalArgumentException("问题须为1至1200字符");
        if(!"RAG".equals(mode)) throw new IllegalStateException("任务三尚未合并，当前只开放RAG模式；GraphRAG与HYBRID未启用");
        var candidates=rows(owner);
        String name="向量检索 · "+models.embeddingModel()+" · 本地MySQL余弦TopK";
        if(candidates.isEmpty()) return new RagTypes.Answer("资料不足：请先上传文件并完成向量索引。",List.of(),List.of(),name,models.chatModel());
        if(candidates.stream().anyMatch(r->!models.embeddingModel().equals(r.get("embedding_model"))))
            throw new IllegalStateException("嵌入模型已更换，请更新文件重建向量后再查询");
        double[] query=models.embed(owner,List.of(question)).get(0);
        List<Map.Entry<Map<String,Object>,Double>> scored=new ArrayList<>();
        for(var row:candidates) {
            double score=cosine(query,json.readValue(row.get("embedding").toString(),double[].class));
            if(score>=minimumScore) scored.add(Map.entry(row,score));
        }
        scored.sort(Map.Entry.<Map<String,Object>,Double>comparingByValue().reversed());
        List<RagTypes.Source> sources=new ArrayList<>();
        for(var entry:scored.stream().limit(4).toList()) {
            var row=entry.getKey(); sources.add(new RagTypes.Source("S"+(sources.size()+1),row.get("document_id").toString(),row.get("title").toString(),((Number)row.get("version")).intValue(),row.get("chunk_id").toString(),row.get("content").toString(),row.get("location").toString(),entry.getValue()));
        }
        if(sources.isEmpty()) return new RagTypes.Answer("资料不足：当前私有库没有达到相关度阈值的依据，请补充资料或换一种问法。",List.of(),List.of(),name,models.chatModel());
        validateCurrent(owner,sources);
        String answer=models.answer(owner,question,sources);
        validateCurrent(owner,sources);
        return new RagTypes.Answer(answer,sources,List.of(),name,models.chatModel());
    }
    private void validateCurrent(long owner,List<RagTypes.Source> sources) {
        for(var source:sources) {
            Integer count=db.queryForObject("SELECT COUNT(*) FROM rag_vector v JOIN rag_document d ON v.owner=d.owner AND v.document_id=d.document_id AND v.version=d.active_version WHERE v.owner=? AND v.document_id=? AND v.version=? AND v.chunk_id=? AND d.deleted=FALSE",Integer.class,owner,source.documentId(),source.version(),source.chunkId());
            if(count==null||count!=1) throw new IllegalStateException("回答期间资料版本已变化，请重新提问");
        }
    }
}
