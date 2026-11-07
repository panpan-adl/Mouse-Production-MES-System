package com.mouse.mes.knowledge.rag;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RagSchema {
    private final JdbcTemplate db;
    public RagSchema(JdbcTemplate db) { this.db = db; }
    @PostConstruct public void initialize() {
        db.execute("CREATE TABLE IF NOT EXISTS rag_document (owner BIGINT NOT NULL, document_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, version INT NOT NULL DEFAULT 0, active_version INT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE, PRIMARY KEY(owner,document_id)) ENGINE=InnoDB");
        db.execute("CREATE TABLE IF NOT EXISTS rag_revision (owner BIGINT NOT NULL, document_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, version INT NOT NULL, title VARCHAR(200) NOT NULL, filename VARCHAR(255) NOT NULL, storage_path VARCHAR(512) NOT NULL, action VARCHAR(16) NOT NULL DEFAULT 'UPSERT', status VARCHAR(24) NOT NULL, error VARCHAR(500) NOT NULL DEFAULT '', attempts INT NOT NULL DEFAULT 0, build_id VARCHAR(64) NOT NULL, parsed_text MEDIUMTEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(owner,document_id,version)) ENGINE=InnoDB");
        db.execute("CREATE TABLE IF NOT EXISTS rag_vector (owner BIGINT NOT NULL, document_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, version INT NOT NULL, chunk_id VARCHAR(96) NOT NULL, title VARCHAR(200) NOT NULL, content TEXT NOT NULL, location TEXT NOT NULL, embedding_model VARCHAR(200) NOT NULL, embedding JSON NOT NULL, PRIMARY KEY(owner,chunk_id), INDEX rag_document_version(owner,document_id,version)) ENGINE=InnoDB");
        db.execute("CREATE TABLE IF NOT EXISTS rag_usage (owner BIGINT NOT NULL, day DATE NOT NULL, calls INT NOT NULL DEFAULT 0, PRIMARY KEY(owner,day)) ENGINE=InnoDB");
        db.execute("CREATE TABLE IF NOT EXISTS rag_graph_sync (owner BIGINT NOT NULL,document_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,version INT NOT NULL,status VARCHAR(24) NOT NULL,error VARCHAR(500) NOT NULL DEFAULT '',attempts INT NOT NULL DEFAULT 0,PRIMARY KEY(owner,document_id,version)) ENGINE=InnoDB");
        // A terminated process cannot leave a permanently 'working' revision.
        db.update("UPDATE rag_revision SET status='FAILED',error='上次处理超时，请重试同一版本' WHERE status IN ('UPLOADED','INDEXING') AND created_at<TIMESTAMPADD(MINUTE,-30,CURRENT_TIMESTAMP)");
    }
}
