USE mouse_mes;

CREATE TABLE IF NOT EXISTS t_qc_inspection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inspection_no VARCHAR(50) NOT NULL UNIQUE,
    inspection_type VARCHAR(30) NOT NULL,
    source_order_id BIGINT,
    source_order_no VARCHAR(80),
    sample_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    qualified_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    defective_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    qualified_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    result VARCHAR(30),
    defect_reason VARCHAR(500),
    handle_method VARCHAR(500),
    business_status VARCHAR(50),
    remark VARCHAR(500),
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_qc_inspection_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inspection_id BIGINT NOT NULL,
    material_id BIGINT,
    lot_id BIGINT,
    inspection_item VARCHAR(100) NOT NULL,
    inspection_standard VARCHAR(500),
    actual_value VARCHAR(500),
    sample_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    qualified_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    defective_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    item_result VARCHAR(30),
    defect_type VARCHAR(100),
    defect_reason VARCHAR(500),
    handle_method VARCHAR(500),
    remark VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS add_qc_column_if_missing;

DELIMITER //
CREATE PROCEDURE add_qc_column_if_missing(
    IN table_name_param VARCHAR(64),
    IN column_name_param VARCHAR(64),
    IN column_definition_param TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_param
          AND column_name = column_name_param
    ) THEN
        SET @add_column_sql = CONCAT('ALTER TABLE ', table_name_param, ' ADD COLUMN ', column_name_param, ' ', column_definition_param);
        PREPARE add_column_stmt FROM @add_column_sql;
        EXECUTE add_column_stmt;
        DEALLOCATE PREPARE add_column_stmt;
    END IF;
END//
DELIMITER ;

CALL add_qc_column_if_missing('t_qc_inspection', 'qualified_rate', 'DECIMAL(5,2) NOT NULL DEFAULT 0');
CALL add_qc_column_if_missing('t_qc_inspection', 'defect_reason', 'VARCHAR(500)');
CALL add_qc_column_if_missing('t_qc_inspection', 'handle_method', 'VARCHAR(500)');
CALL add_qc_column_if_missing('t_qc_inspection', 'business_status', 'VARCHAR(50)');

CALL add_qc_column_if_missing('t_qc_inspection_item', 'inspection_item', 'VARCHAR(100) NOT NULL DEFAULT ''未命名检测项''');
CALL add_qc_column_if_missing('t_qc_inspection_item', 'inspection_standard', 'VARCHAR(500)');
CALL add_qc_column_if_missing('t_qc_inspection_item', 'actual_value', 'VARCHAR(500)');
CALL add_qc_column_if_missing('t_qc_inspection_item', 'item_result', 'VARCHAR(30)');
CALL add_qc_column_if_missing('t_qc_inspection_item', 'defect_reason', 'VARCHAR(500)');
CALL add_qc_column_if_missing('t_qc_inspection_item', 'handle_method', 'VARCHAR(500)');

ALTER TABLE t_qc_inspection_item MODIFY material_id BIGINT NULL;

DROP PROCEDURE add_qc_column_if_missing;
