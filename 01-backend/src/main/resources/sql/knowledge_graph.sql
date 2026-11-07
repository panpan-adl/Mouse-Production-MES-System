-- Additive schema only. Applied only when MES_KNOWLEDGE_ENABLED=true.
CREATE TABLE IF NOT EXISTS mm_kg_document (
    owner_id BIGINT NOT NULL,
    document_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    title VARCHAR(200) NOT NULL,
    version BIGINT NOT NULL,
    content MEDIUMTEXT NOT NULL,
    build_id VARCHAR(36) NOT NULL,
    chunks_json MEDIUMTEXT NOT NULL,
    state VARCHAR(16) NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (owner_id, document_id),
    INDEX idx_kg_owner_state (owner_id, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
