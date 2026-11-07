package com.mouse.mes.knowledge;

import com.mouse.mes.knowledge.rag.RagSchema;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Shared chunk text is identical to the parser's chunk text; retain PDF pages/DOCX paragraphs. */
@Component
public class KnowledgeSourceLocations {
    private final JdbcTemplate db;
    public KnowledgeSourceLocations(JdbcTemplate db,RagSchema schema){this.db=db;}
    public String resolve(long owner,KnowledgeTypes.Chunk chunk){
        var rows=db.queryForList("SELECT v.content,v.location FROM rag_vector v JOIN rag_document d ON d.owner=v.owner AND d.document_id=v.document_id AND d.active_version=v.version WHERE v.owner=? AND v.document_id=? AND v.version=? AND d.deleted=FALSE",owner,chunk.documentId(),chunk.version());
        for(var row:rows)if(chunk.text().equals(row.get("content")))return row.get("location").toString();
        return "片段"+(chunk.ordinal()+1);
    }
}
