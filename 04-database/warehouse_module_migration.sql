USE mouse_mes;

CREATE TABLE IF NOT EXISTS t_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    location_code VARCHAR(50) NOT NULL,
    location_name VARCHAR(100) NOT NULL,
    location_type VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT '1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT,
    material_id BIGINT NOT NULL,
    lot_id BIGINT,
    quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    last_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_stock_in (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_in_no VARCHAR(80) NOT NULL UNIQUE,
    stock_in_type VARCHAR(30) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    source_order_id BIGINT,
    source_order_no VARCHAR(80),
    total_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    remark VARCHAR(500),
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_stock_in_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_in_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    lot_id BIGINT,
    quantity DECIMAL(10,2) NOT NULL,
    location_id BIGINT,
    remark VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_stock_out (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_out_no VARCHAR(80) NOT NULL UNIQUE,
    stock_out_type VARCHAR(30) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    source_order_id BIGINT,
    source_order_no VARCHAR(80),
    total_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    remark VARCHAR(500),
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_stock_out_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_out_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    lot_id BIGINT,
    quantity DECIMAL(10,2) NOT NULL,
    location_id BIGINT,
    remark VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_inventory_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_no VARCHAR(120) NOT NULL UNIQUE,
    transaction_type VARCHAR(10) NOT NULL,
    business_type VARCHAR(30),
    source_order_id BIGINT,
    source_order_no VARCHAR(80),
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT,
    material_id BIGINT NOT NULL,
    lot_id BIGINT,
    quantity DECIMAL(10,2) NOT NULL,
    before_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    after_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
