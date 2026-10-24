USE mouse_mes;

CREATE TABLE IF NOT EXISTS t_equipment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    equipment_code VARCHAR(50) NOT NULL UNIQUE,
    equipment_name VARCHAR(100) NOT NULL,
    equipment_type VARCHAR(50),
    model VARCHAR(50),
    manufacturer VARCHAR(100),
    purchase_date DATE,
    warranty_period INT,
    location VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    responsible_id BIGINT,
    responsible_name VARCHAR(50),
    remark VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_maintenance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    maintenance_no VARCHAR(50) NOT NULL UNIQUE,
    equipment_id BIGINT NOT NULL,
    equipment_name VARCHAR(100) NOT NULL,
    maintenance_type TINYINT NOT NULL DEFAULT 2,
    fault_desc TEXT,
    maintenance_content TEXT,
    maintenance_user_id BIGINT,
    maintenance_user_name VARCHAR(50),
    start_time DATETIME,
    end_time DATETIME,
    cost DECIMAL(10,2),
    result VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    remark VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS add_equipment_column_if_missing;

DELIMITER //
CREATE PROCEDURE add_equipment_column_if_missing(
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

CALL add_equipment_column_if_missing('t_equipment', 'is_deleted', 'TINYINT NOT NULL DEFAULT 0');
CALL add_equipment_column_if_missing('t_maintenance', 'status', 'VARCHAR(30) NOT NULL DEFAULT ''PENDING''');
CALL add_equipment_column_if_missing('t_maintenance', 'update_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

ALTER TABLE t_equipment MODIFY status VARCHAR(30) NOT NULL DEFAULT 'NORMAL';
UPDATE t_equipment
SET status = CASE status
    WHEN '0' THEN 'SCRAPPED'
    WHEN '1' THEN 'NORMAL'
    WHEN '2' THEN 'MAINTAINING'
    WHEN '3' THEN 'FAULT'
    ELSE status
END;

ALTER TABLE t_maintenance MODIFY maintenance_content TEXT NULL;
ALTER TABLE t_maintenance MODIFY maintenance_user_id BIGINT NULL;
ALTER TABLE t_maintenance MODIFY maintenance_user_name VARCHAR(50) NULL;
ALTER TABLE t_maintenance MODIFY start_time DATETIME NULL;
ALTER TABLE t_maintenance MODIFY result VARCHAR(50) NULL;
UPDATE t_maintenance
SET status = CASE result
    WHEN '1' THEN 'FINISHED'
    ELSE status
END
WHERE status IS NULL OR status = '' OR status = 'PENDING';

DROP PROCEDURE add_equipment_column_if_missing;
