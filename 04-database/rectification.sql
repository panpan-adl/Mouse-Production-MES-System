CREATE TABLE IF NOT EXISTS t_material_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '物料请求ID，主键自增',
    request_no VARCHAR(50) NOT NULL UNIQUE COMMENT '物料请求编号',
    required_date DATE NULL,
    source_task_id BIGINT COMMENT '来源生产任务ID',
    source_task_no VARCHAR(50) COMMENT '来源生产任务编号',
    product_name VARCHAR(100) COMMENT '产品名称',
    material_id BIGINT NOT NULL COMMENT '需求物料ID',
    material_code VARCHAR(50) COMMENT '物料编码',
    material_name VARCHAR(100) COMMENT '物料名称',
    required_quantity DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '需求数量',
    available_quantity DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '可用库存数量',
    shortage_quantity DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '缺料数量',
    warehouse_id BIGINT COMMENT '建议出料仓库ID',
    request_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_CHECK' COMMENT '状态：PENDING_CHECK待确认 ENOUGH库存充足 SHORTAGE库存不足 PURCHASE_REQUESTED已发采购 PURCHASE_ORDERED已转采购单',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_material_request_status(request_status),
    INDEX idx_material_request_material(material_id),
    INDEX idx_material_request_task(source_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产物料请求表';
CREATE TABLE IF NOT EXISTS mm_maintenance_plan (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,name VARCHAR(100) NOT NULL,equipment_id BIGINT NOT NULL,owner_id BIGINT NOT NULL,
 anchor_at DATETIME NOT NULL,frequency INT NOT NULL,cycle_unit VARCHAR(10) NOT NULL,remind_days INT NOT NULL DEFAULT 0,
 content VARCHAR(500) NOT NULL,enabled INT NOT NULL DEFAULT 1,next_index BIGINT NOT NULL DEFAULT 0,last_error VARCHAR(500)
);
CREATE TABLE IF NOT EXISTS mm_maintenance_occurrence (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,plan_id BIGINT NOT NULL,occurrence_index BIGINT NOT NULL,maintenance_id BIGINT NOT NULL,
 planned_at DATETIME NOT NULL,UNIQUE KEY uq_plan_occurrence(plan_id,occurrence_index),UNIQUE KEY uq_maintenance(maintenance_id)
);
CREATE TABLE IF NOT EXISTS mm_role_profile (role_id BIGINT PRIMARY KEY,base_role_id BIGINT NOT NULL);
