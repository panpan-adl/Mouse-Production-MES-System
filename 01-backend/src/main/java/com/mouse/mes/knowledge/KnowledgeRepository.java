package com.mouse.mes.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
import static com.mouse.mes.knowledge.KnowledgeTypes.*;

@Repository
public class KnowledgeRepository {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    public KnowledgeRepository(JdbcTemplate db, ObjectMapper json) { this.db=db; this.json=json; }
    public List<Document> list(long owner) {
        return db.query("SELECT * FROM mm_kg_document WHERE owner_id=? AND state='READY' ORDER BY title,document_id",
                (rs,n)->new Document(rs.getString("document_id"),rs.getString("title"),rs.getLong("version"),rs.getString("content"),
                        rs.getString("build_id"),decode(rs.getString("chunks_json")),rs.getString("state")),owner);
    }
    public Document lock(long owner, String id) {
        db.update("INSERT IGNORE INTO mm_kg_document(owner_id,document_id,title,version,content,build_id,chunks_json,state) VALUES (?,?,'',0,'','','[]','NEW')",owner,id);
        return db.queryForObject("SELECT * FROM mm_kg_document WHERE owner_id=? AND document_id=? FOR UPDATE",
                (rs,n)->new Document(id,rs.getString("title"),rs.getLong("version"),rs.getString("content"),rs.getString("build_id"),
                        decode(rs.getString("chunks_json")),rs.getString("state")),owner,id);
    }
    public void save(long owner, Document doc) {
        try {
            db.update("UPDATE mm_kg_document SET title=?,version=?,content=?,build_id=?,chunks_json=?,state=?,updated_at=CURRENT_TIMESTAMP(6) WHERE owner_id=? AND document_id=?",
                    doc.title(),doc.version(),doc.text(),doc.buildId(),json.writeValueAsString(doc.chunks()),doc.state(),owner,doc.documentId());
        } catch (java.io.IOException e) { throw new IllegalStateException("文件分块保存失败"); }
    }
    public List<Map<String,Object>> pendingDeletions(long owner) {
        return db.query("SELECT document_id,title,version FROM mm_kg_document WHERE owner_id=? AND state='DELETED_PENDING' ORDER BY updated_at",
                (rs,n)->Map.<String,Object>of("documentId",rs.getString("document_id"),"title",rs.getString("title"),"version",rs.getLong("version")),owner);
    }
    public void finishDeletion(long owner,String id,long version) {
        db.update("UPDATE mm_kg_document SET state='DELETED',updated_at=CURRENT_TIMESTAMP(6) WHERE owner_id=? AND document_id=? AND version=? AND state='DELETED_PENDING'",owner,id,version);
    }
    private List<Chunk> decode(String value) {
        try { return json.readValue(value,new TypeReference<List<Chunk>>(){}); }
        catch (java.io.IOException e) { throw new IllegalStateException("文件分块读取失败"); }
    }
}
