-- Apply to the selected MES database.
CREATE TABLE IF NOT EXISTS mm_face_template (
    user_id BIGINT NOT NULL,
    model_version VARCHAR(80) NOT NULL,
    key_id VARCHAR(64) NOT NULL,
    nonce BINARY(12) NOT NULL,
    encrypted_embedding BLOB NOT NULL,
    enrolled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_face_template_nonce (key_id, nonce),
    KEY idx_face_template_model (model_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
