-- Additive, idempotent migration. Never invent an opening balance for existing materials.
SET @location_status_sql=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mm_location' AND COLUMN_NAME='status')=0,
 'ALTER TABLE mm_location ADD COLUMN status INT NOT NULL DEFAULT 1','SELECT 1');
PREPARE location_status_stmt FROM @location_status_sql;
EXECUTE location_status_stmt;
DEALLOCATE PREPARE location_status_stmt;
UPDATE mm_location l JOIN mm_area a ON a.id=l.area_id JOIN t_location t ON t.warehouse_id=a.warehouse_id AND t.location_code=l.code SET l.status=IF(t.status='1' AND t.is_deleted=0,1,0);
CREATE TABLE IF NOT EXISTS mm_material_origin (
 material_id BIGINT PRIMARY KEY,created_at DATETIME(6) NOT NULL,ledger_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS mm_qc_comparison (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,task_id BIGINT NOT NULL,mode VARCHAR(10) NOT NULL,
 started_by BIGINT NOT NULL,started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 finished_at DATETIME(6) NULL,todo_id BIGINT NULL,
 UNIQUE KEY(task_id,mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
