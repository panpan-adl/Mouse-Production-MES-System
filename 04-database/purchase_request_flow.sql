-- Explicit provenance; no existing business records are rewritten or inferred from remarks.
CREATE TABLE IF NOT EXISTS mm_purchase_request_order (
  request_id BIGINT NOT NULL PRIMARY KEY,
  po_id BIGINT NOT NULL UNIQUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
