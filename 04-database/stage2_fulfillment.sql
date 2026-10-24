-- Additive migration: existing balances and documents are preserved.
-- Existing purchase mapper expects this optional legacy link; add it only when absent.
SET @purchase_link_sql=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_purchase_order' AND COLUMN_NAME='sales_order_id')=0,
 'ALTER TABLE t_purchase_order ADD COLUMN sales_order_id BIGINT NULL','SELECT 1');
PREPARE purchase_link FROM @purchase_link_sql;
EXECUTE purchase_link;
DEALLOCATE PREPARE purchase_link;
CREATE TABLE IF NOT EXISTS mm_work_material (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,work_order_id BIGINT NOT NULL,material_id BIGINT NOT NULL,
 material_code VARCHAR(60) NOT NULL,material_name VARCHAR(100) NOT NULL,unit VARCHAR(20) NOT NULL,
 required_quantity DECIMAL(14,2) NOT NULL,UNIQUE KEY(work_order_id,material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS mm_work_product (
 work_order_id BIGINT PRIMARY KEY,material_id BIGINT NOT NULL,quantity INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS mm_stock_posting (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,kind VARCHAR(30) NOT NULL,source_id BIGINT NOT NULL,
 document_id BIGINT NOT NULL,document_no VARCHAR(80) NOT NULL,warehouse_id BIGINT NOT NULL,
 location_id BIGINT NULL,quantity DECIMAL(14,2) NOT NULL,actor_id BIGINT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE KEY(kind,source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
