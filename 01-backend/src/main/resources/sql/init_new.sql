DROP TABLE IF EXISTS t_attendance;
DROP TABLE IF EXISTS t_bom;
DROP TABLE IF EXISTS t_customer;
DROP TABLE IF EXISTS t_department;
DROP TABLE IF EXISTS t_equipment;
DROP TABLE IF EXISTS t_inventory;
DROP TABLE IF EXISTS t_inventory_transaction;
DROP TABLE IF EXISTS t_inventory_in;
DROP TABLE IF EXISTS t_inventory_out;
DROP TABLE IF EXISTS t_location;
DROP TABLE IF EXISTS t_login_log;
DROP TABLE IF EXISTS t_maintenance;
DROP TABLE IF EXISTS t_material;
DROP TABLE IF EXISTS t_order;
DROP TABLE IF EXISTS t_operation_log;
DROP TABLE IF EXISTS t_product;
DROP TABLE IF EXISTS t_production_order;
DROP TABLE IF EXISTS t_production_plan;
DROP TABLE IF EXISTS t_production_report;
DROP TABLE IF EXISTS t_production_task;
DROP TABLE IF EXISTS t_purchase_order_item;
DROP TABLE IF EXISTS t_purchase_order;
DROP TABLE IF EXISTS t_qc_inspection_item;
DROP TABLE IF EXISTS t_qc_inspection;
DROP TABLE IF EXISTS t_quality_check;
DROP TABLE IF EXISTS t_quality_standard;
DROP TABLE IF EXISTS t_sales_order_item;
DROP TABLE IF EXISTS t_sales_order;
DROP TABLE IF EXISTS t_stock_in_item;
DROP TABLE IF EXISTS t_stock_in;
DROP TABLE IF EXISTS t_stock_out_item;
DROP TABLE IF EXISTS t_stock_out;
DROP TABLE IF EXISTS t_supplier;
DROP TABLE IF EXISTS t_uom;
DROP TABLE IF EXISTS t_user;
DROP TABLE IF EXISTS t_warehouse;
DROP TABLE IF EXISTS t_work_order;
DROP TABLE IF EXISTS t_role;

-- t_role（角色表）
CREATE TABLE IF NOT EXISTS t_role (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID，主键自增',
    role_code VARCHAR(10) NOT NULL UNIQUE COMMENT '角色编码（如101、109），唯一',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称（如生产经理、CEO）',
    role_desc VARCHAR(255) COMMENT '角色描述',
    sort INT NOT NULL COMMENT '显示顺序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用，默认1',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- t_department（部门表）
CREATE TABLE IF NOT EXISTS t_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID，主键自增',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID，顶级为0',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称',
    dept_code VARCHAR(50) NOT NULL COMMENT '部门编码',
    leader_id BIGINT COMMENT '部门负责人ID',
    leader_name VARCHAR(50) COMMENT '部门负责人姓名',
    sort INT NOT NULL COMMENT '显示顺序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：-1删除 0禁用 1活跃，默认1',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- t_user（用户表）
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID，主键自增',
    user_code VARCHAR(6) NOT NULL UNIQUE COMMENT '6位用户编码（前3位角色+后3位序号），唯一',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号，唯一',
    password VARCHAR(255) NOT NULL COMMENT '登录密码（加密存储）',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号码',
    email VARCHAR(100) COMMENT '邮箱地址',
    gender TINYINT COMMENT '性别：0女 1男',
    avatar VARCHAR(255) COMMENT '头像URL',
    role_id INT NOT NULL COMMENT '角色ID（关联t_role.id）',
    dept_id BIGINT COMMENT '部门ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0离职 1在职 2试用期，默认1',
    entry_date DATE COMMENT '入职日期',
    leave_date DATE COMMENT '离职日期',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除，默认0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- t_login_log（登录日志表）
CREATE TABLE IF NOT EXISTS t_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID，主键自增',
    user_id BIGINT COMMENT '用户ID',
    user_code VARCHAR(6) NOT NULL COMMENT '用户编码',
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    login_type TINYINT NOT NULL COMMENT '操作类型：1登录 2登出',
    login_ip VARCHAR(50) COMMENT '登录IP',
    login_location VARCHAR(100) COMMENT '登录地点',
    browser VARCHAR(255) COMMENT '浏览器类型',
    os VARCHAR(255) COMMENT '操作系统',
    status TINYINT NOT NULL COMMENT '登录状态：0失败 1成功',
    msg VARCHAR(255) COMMENT '提示消息',
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- t_operation_log（操作日志表）
CREATE TABLE IF NOT EXISTS t_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID，主键自增',
    user_id BIGINT COMMENT '操作人ID',
    user_code VARCHAR(6) NOT NULL COMMENT '操作人编码',
    username VARCHAR(50) NOT NULL COMMENT '操作人账号',
    module VARCHAR(50) NOT NULL COMMENT '操作模块',
    operation VARCHAR(100) NOT NULL COMMENT '操作描述',
    method VARCHAR(200) COMMENT '请求方法',
    request_url VARCHAR(255) COMMENT '请求URL',
    request_param TEXT COMMENT '请求参数',
    response_result TEXT COMMENT '返回结果',
    operate_ip VARCHAR(50) COMMENT '操作IP',
    operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    status TINYINT NOT NULL COMMENT '操作状态：0失败 1成功',
    error_msg TEXT COMMENT '错误消息'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- t_dict（字典类型表）
CREATE TABLE IF NOT EXISTS t_dict (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典ID，主键自增',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_type VARCHAR(100) NOT NULL UNIQUE COMMENT '字典类型，唯一',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用，默认1',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- t_dict_item（字典数据表）
CREATE TABLE IF NOT EXISTS t_dict_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典数据ID，主键自增',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型',
    item_label VARCHAR(100) NOT NULL COMMENT '字典标签',
    item_value VARCHAR(100) NOT NULL COMMENT '字典键值',
    sort INT NOT NULL COMMENT '显示顺序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用，默认1',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- t_product（产品表）
CREATE TABLE IF NOT EXISTS t_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '产品ID，主键自增',
    product_code VARCHAR(50) NOT NULL UNIQUE COMMENT '产品编码，唯一',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    product_type VARCHAR(50) COMMENT '产品类型',
    model VARCHAR(50) COMMENT '产品型号',
    specification VARCHAR(100) COMMENT '规格参数',
    unit VARCHAR(20) NOT NULL COMMENT '计量单位',
    cost_price DECIMAL(10,2) COMMENT '成本价',
    selling_price DECIMAL(10,2) COMMENT '销售价',
    description TEXT COMMENT '产品描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停售 1在售，默认1',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

-- t_order（订单表）
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID，主键自增',
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单编号，唯一',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    customer_phone VARCHAR(20) COMMENT '客户电话',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    quantity INT NOT NULL COMMENT '订单数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    order_date DATE NOT NULL COMMENT '下单日期',
    delivery_date DATE COMMENT '交付日期',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待确认 1已确认 2生产中 3已完成 4已取消',
    priority TINYINT NOT NULL DEFAULT 2 COMMENT '优先级：1低 2中 3高，默认2',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT NOT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除，默认0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- t_order_product（订单产品明细表）
CREATE TABLE IF NOT EXISTS t_order_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID，主键自增',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    quantity INT NOT NULL COMMENT '数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    subtotal DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单产品明细表';

-- t_sales_order（销售订单表）
CREATE TABLE IF NOT EXISTS t_sales_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '销售订单ID',
    so_no VARCHAR(50) NOT NULL UNIQUE COMMENT '销售订单编号',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    order_date DATETIME COMMENT '下单日期',
    delivery_date DATETIME COMMENT '交付日期',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '总金额',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单表';

-- t_sales_order_item（销售订单明细表）
CREATE TABLE IF NOT EXISTS t_sales_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '销售订单明细ID',
    so_id BIGINT NOT NULL COMMENT '销售订单ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    quantity DECIMAL(10,2) NOT NULL COMMENT '数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '金额',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单明细表';

-- t_production_order（生产订单表）
CREATE TABLE IF NOT EXISTS t_production_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '生产订单ID，主键自增',
    po_no VARCHAR(50) NOT NULL UNIQUE COMMENT '生产订单编号，唯一',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    planned_quantity DECIMAL(10,2) NOT NULL COMMENT '计划数量',
    actual_quantity DECIMAL(10,2) DEFAULT 0 COMMENT '实际数量',
    planned_start_date DATE COMMENT '计划开始日期',
    planned_end_date DATE COMMENT '计划结束日期',
    actual_start_date DATETIME COMMENT '实际开始时间',
    actual_end_date DATETIME COMMENT '实际结束时间',
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT '状态：PLANNED计划中 IN_PROGRESS生产中 COMPLETED已完成',
    department_id BIGINT COMMENT '部门ID',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产订单表';

-- t_production_plan（生产计划表）
CREATE TABLE IF NOT EXISTS t_production_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '计划ID，主键自增',
    plan_no VARCHAR(50) NOT NULL UNIQUE COMMENT '计划编号，唯一',
    order_id BIGINT COMMENT '关联订单ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    plan_quantity INT NOT NULL COMMENT '计划数量',
    completed_quantity INT NOT NULL DEFAULT 0 COMMENT '完成数量，默认0',
    start_date DATE COMMENT '计划开始日期',
    end_date DATE COMMENT '计划结束日期',
    plan_status TINYINT NOT NULL DEFAULT 0 COMMENT '计划状态：0待开始 1进行中 2已完成 3已暂停',
    priority TINYINT NOT NULL DEFAULT 2 COMMENT '优先级：1低 2中 3高，默认2',
    create_by BIGINT NOT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产计划表';

-- t_production_task（生产任务表）
CREATE TABLE IF NOT EXISTS t_production_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID，主键自增',
    task_no VARCHAR(50) NOT NULL UNIQUE COMMENT '任务编号，唯一',
    plan_id BIGINT NOT NULL COMMENT '生产计划ID',
    task_type TINYINT NOT NULL COMMENT '任务类型：1加工 2组装',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    quantity INT NOT NULL COMMENT '任务数量',
    assigned_user_id BIGINT COMMENT '分配给的用户ID',
    assigned_user_name VARCHAR(50) COMMENT '分配给的用户姓名',
    task_status TINYINT NOT NULL DEFAULT 0 COMMENT '任务状态：0待领取 1进行中 2已完成 3已暂停',
    start_time DATETIME COMMENT '实际开始时间',
    end_time DATETIME COMMENT '实际结束时间',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产任务表';

-- t_production_report（生产报工表）
CREATE TABLE IF NOT EXISTS t_production_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报工ID，主键自增',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    report_user_id BIGINT NOT NULL COMMENT '报工人ID',
    report_user_name VARCHAR(50) NOT NULL COMMENT '报工人姓名',
    report_quantity INT NOT NULL COMMENT '本次完成数量',
    qualified_quantity INT NOT NULL COMMENT '合格数量',
    defect_quantity INT NOT NULL COMMENT '不合格数量',
    report_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报工时间',
    remark VARCHAR(500) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产报工表';

-- t_supplier（供应商表）
CREATE TABLE IF NOT EXISTS t_supplier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '供应商ID，主键自增',
    supplier_code VARCHAR(50) NOT NULL UNIQUE COMMENT '供应商编码，唯一',
    supplier_name VARCHAR(100) NOT NULL COMMENT '供应商名称',
    contact_person VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    address VARCHAR(255) COMMENT '地址',
    bank_name VARCHAR(100) COMMENT '开户银行',
    bank_account VARCHAR(50) COMMENT '银行账号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：-1删除 0禁用 1活跃，默认1',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- t_purchase_order（采购订单表）
CREATE TABLE IF NOT EXISTS t_purchase_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '采购单ID，主键自增',
    po_no VARCHAR(50) NOT NULL UNIQUE COMMENT '采购单编号，唯一',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    order_date DATETIME COMMENT '采购日期',
    expected_date DATE COMMENT '预计到货日期',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '总金额',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿 SUBMITTED已提交 APPROVED已审核 PARTIAL_RECEIVED部分到货 COMPLETED全部到货 CANCELLED已取消',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单表';

-- t_purchase_order_item（采购订单明细表）
CREATE TABLE IF NOT EXISTS t_purchase_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID，主键自增',
    po_id BIGINT NOT NULL COMMENT '采购单ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    quantity DECIMAL(10,2) NOT NULL COMMENT '采购数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '小计金额',
    received_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '已到货数量',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单明细表';

-- t_material（物料表）
CREATE TABLE IF NOT EXISTS t_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '物料ID，主键自增',
    material_code VARCHAR(50) NOT NULL UNIQUE COMMENT '物料编码，唯一',
    material_name VARCHAR(100) NOT NULL COMMENT '物料名称',
    material_type TINYINT NOT NULL COMMENT '物料类型：1原材料 2半成品 3成品',
    specification VARCHAR(100) COMMENT '规格型号',
    unit VARCHAR(20) NOT NULL COMMENT '计量单位',
    safety_stock INT COMMENT '安全库存',
    current_stock INT NOT NULL DEFAULT 0 COMMENT '当前库存，默认0',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用，默认1',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料表';

-- t_warehouse（仓库表）
CREATE TABLE IF NOT EXISTS t_warehouse (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '仓库ID，主键自增',
    warehouse_code VARCHAR(50) NOT NULL UNIQUE COMMENT '仓库编码，唯一',
    warehouse_name VARCHAR(100) NOT NULL COMMENT '仓库名称',
    location VARCHAR(255) COMMENT '仓库位置',
    manager_id BIGINT COMMENT '仓管员ID',
    manager_name VARCHAR(50) COMMENT '仓管员姓名',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用，默认1',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库表';

-- t_location（库位表）
CREATE TABLE IF NOT EXISTS t_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库位ID，主键自增',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    location_code VARCHAR(50) NOT NULL COMMENT '库位编码',
    location_name VARCHAR(100) NOT NULL COMMENT '库位名称',
    location_type VARCHAR(30) COMMENT '库位类型',
    status VARCHAR(30) NOT NULL DEFAULT '1' COMMENT '状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位表';

-- t_inventory（物料库存表）
CREATE TABLE IF NOT EXISTS t_inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存ID，主键自增',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    location_id BIGINT COMMENT '库位ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    lot_id BIGINT COMMENT '批次ID',
    quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '库存数量',
    last_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料库存表';

-- t_inventory_in（入库单表）
CREATE TABLE IF NOT EXISTS t_inventory_in (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '入库单ID，主键自增',
    in_no VARCHAR(50) NOT NULL UNIQUE COMMENT '入库单号，唯一',
    in_type TINYINT NOT NULL COMMENT '入库类型：1采购入库 2生产入库 3退货入库',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    material_name VARCHAR(100) NOT NULL COMMENT '物料名称',
    quantity INT NOT NULL COMMENT '入库数量',
    unit_price DECIMAL(10,2) COMMENT '单价',
    total_amount DECIMAL(10,2) COMMENT '总金额',
    supplier_id BIGINT COMMENT '供应商ID（采购入库时）',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    in_date DATE NOT NULL COMMENT '入库日期',
    create_by BIGINT NOT NULL COMMENT '操作人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    remark VARCHAR(500) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单表';

-- t_inventory_out（出库单表）
CREATE TABLE IF NOT EXISTS t_inventory_out (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '出库单ID，主键自增',
    out_no VARCHAR(50) NOT NULL UNIQUE COMMENT '出库单号，唯一',
    out_type TINYINT NOT NULL COMMENT '出库类型：1生产领料 2销售出库 3报废出库',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    material_name VARCHAR(100) NOT NULL COMMENT '物料名称',
    quantity INT NOT NULL COMMENT '出库数量',
    unit_price DECIMAL(10,2) COMMENT '单价',
    total_amount DECIMAL(10,2) COMMENT '总金额',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    out_date DATE NOT NULL COMMENT '出库日期',
    create_by BIGINT NOT NULL COMMENT '操作人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    remark VARCHAR(500) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单表';

-- t_stock_in（通用入库单表，采购到货时自动生成）
CREATE TABLE IF NOT EXISTS t_stock_in (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '入库单ID，主键自增',
    stock_in_no VARCHAR(80) NOT NULL UNIQUE COMMENT '入库单号，唯一',
    stock_in_type VARCHAR(30) NOT NULL COMMENT '入库类型，例如 PURCHASE',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    source_order_id BIGINT COMMENT '来源订单ID',
    source_order_no VARCHAR(80) COMMENT '来源订单编号',
    total_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '入库总数量',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '入库状态',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用入库单表';

-- t_stock_in_item（通用入库单明细表，记录采购到货的物料行）
CREATE TABLE IF NOT EXISTS t_stock_in_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '入库明细ID，主键自增',
    stock_in_id BIGINT NOT NULL COMMENT '入库单ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    lot_id BIGINT COMMENT '批次ID',
    quantity DECIMAL(10,2) NOT NULL COMMENT '入库数量',
    location_id BIGINT COMMENT '库位ID',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用入库单明细表';

-- t_stock_out（通用出库单表）
CREATE TABLE IF NOT EXISTS t_stock_out (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '出库单ID，主键自增',
    stock_out_no VARCHAR(80) NOT NULL UNIQUE COMMENT '出库单号，唯一',
    stock_out_type VARCHAR(30) NOT NULL COMMENT '出库类型，例如 PRODUCTION_ISSUE、SALES、OTHER',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    source_order_id BIGINT COMMENT '来源订单ID',
    source_order_no VARCHAR(80) COMMENT '来源订单编号',
    total_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '出库总数量',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '出库状态',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用出库单表';

-- t_stock_out_item（通用出库单明细表）
CREATE TABLE IF NOT EXISTS t_stock_out_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '出库明细ID，主键自增',
    stock_out_id BIGINT NOT NULL COMMENT '出库单ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    lot_id BIGINT COMMENT '批次ID',
    quantity DECIMAL(10,2) NOT NULL COMMENT '出库数量',
    location_id BIGINT COMMENT '库位ID',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用出库单明细表';

-- t_inventory_transaction（库存流水表）
CREATE TABLE IF NOT EXISTS t_inventory_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存流水ID，主键自增',
    transaction_no VARCHAR(80) NOT NULL UNIQUE COMMENT '库存流水编号，唯一',
    transaction_type VARCHAR(10) NOT NULL COMMENT '流水方向：IN入库 OUT出库',
    business_type VARCHAR(30) COMMENT '业务类型',
    source_order_id BIGINT COMMENT '来源单据ID',
    source_order_no VARCHAR(80) COMMENT '来源单据编号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    location_id BIGINT COMMENT '库位ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    lot_id BIGINT COMMENT '批次ID',
    quantity DECIMAL(10,2) NOT NULL COMMENT '变动数量',
    before_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '变动前数量',
    after_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '变动后数量',
    created_by BIGINT COMMENT '操作人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    remark VARCHAR(500) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

-- t_quality_standard（质检标准表）
CREATE TABLE IF NOT EXISTS t_quality_standard (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标准ID，主键自增',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    standard_name VARCHAR(100) NOT NULL COMMENT '标准名称',
    standard_content TEXT NOT NULL COMMENT '标准内容',
    qualified_threshold DECIMAL(5,2) COMMENT '合格阈值（%）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用，默认1',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检标准表';

-- t_quality_check（质检记录表）
CREATE TABLE IF NOT EXISTS t_quality_check (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '质检ID，主键自增',
    check_no VARCHAR(50) NOT NULL UNIQUE COMMENT '质检单号，唯一',
    task_id BIGINT COMMENT '关联任务ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    check_quantity INT NOT NULL COMMENT '质检数量',
    qualified_quantity INT NOT NULL COMMENT '合格数量',
    defect_quantity INT NOT NULL COMMENT '不合格数量',
    qualified_rate DECIMAL(5,2) NOT NULL COMMENT '合格率（%）',
    check_result TINYINT NOT NULL COMMENT '质检结果：0不合格 1合格 2部分合格',
    checker_id BIGINT NOT NULL COMMENT '质检员ID',
    checker_name VARCHAR(50) NOT NULL COMMENT '质检员姓名',
    check_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '质检时间',
    defect_desc TEXT COMMENT '缺陷描述',
    handle_result VARCHAR(500) COMMENT '处理结果',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检记录表';

-- t_qc_inspection（质检单表）
CREATE TABLE IF NOT EXISTS t_qc_inspection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '质检单ID',
    inspection_no VARCHAR(50) NOT NULL UNIQUE COMMENT '质检单号',
    inspection_type VARCHAR(30) NOT NULL COMMENT '质检类型：INCOMING/PRODUCTION/FINISHED',
    source_order_id BIGINT COMMENT '来源单据ID',
    source_order_no VARCHAR(80) COMMENT '来源单据编号',
    sample_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '检验数量',
    qualified_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '合格数量',
    defective_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '不合格数量',
    qualified_rate DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '合格率百分比',
    result VARCHAR(30) COMMENT '质检结果',
    defect_reason VARCHAR(500) COMMENT '不合格原因',
    handle_method VARCHAR(500) COMMENT '处理方式',
    business_status VARCHAR(50) COMMENT '后续业务状态',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检单表';

-- t_qc_inspection_item（质检单明细表）
CREATE TABLE IF NOT EXISTS t_qc_inspection_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '质检明细ID',
    inspection_id BIGINT NOT NULL COMMENT '质检单ID',
    material_id BIGINT COMMENT '物料ID',
    lot_id BIGINT COMMENT '批次ID',
    inspection_item VARCHAR(100) NOT NULL COMMENT '检测项',
    inspection_standard VARCHAR(500) COMMENT '检测标准',
    actual_value VARCHAR(500) COMMENT '实测值',
    sample_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '检验数量',
    qualified_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '合格数量',
    defective_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '不合格数量',
    item_result VARCHAR(30) COMMENT '明细结果',
    defect_type VARCHAR(100) COMMENT '缺陷类型',
    defect_reason VARCHAR(500) COMMENT '不合格原因',
    handle_method VARCHAR(500) COMMENT '处理方式',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检单明细表';

-- t_equipment（设备表）
CREATE TABLE IF NOT EXISTS t_equipment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设备ID，主键自增',
    equipment_code VARCHAR(50) NOT NULL UNIQUE COMMENT '设备编码，唯一',
    equipment_name VARCHAR(100) NOT NULL COMMENT '设备名称',
    equipment_type VARCHAR(50) COMMENT '设备类型',
    model VARCHAR(50) COMMENT '设备型号',
    manufacturer VARCHAR(100) COMMENT '生产厂商',
    purchase_date DATE COMMENT '购置日期',
    warranty_period INT COMMENT '保修期（月）',
    location VARCHAR(100) COMMENT '设备位置',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0报废 1正常 2维修中 3停用',
    responsible_id BIGINT COMMENT '负责人ID',
    responsible_name VARCHAR(50) COMMENT '负责人姓名',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

-- t_maintenance（设备维护记录表）
CREATE TABLE IF NOT EXISTS t_maintenance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '维护ID，主键自增',
    maintenance_no VARCHAR(50) NOT NULL UNIQUE COMMENT '维护单号，唯一',
    equipment_id BIGINT NOT NULL COMMENT '设备ID',
    equipment_name VARCHAR(100) NOT NULL COMMENT '设备名称',
    maintenance_type TINYINT NOT NULL COMMENT '维护类型：1保养 2维修 3巡检',
    fault_desc TEXT COMMENT '故障描述（维修时）',
    maintenance_content TEXT NOT NULL COMMENT '维护内容',
    maintenance_user_id BIGINT NOT NULL COMMENT '维护人ID',
    maintenance_user_name VARCHAR(50) NOT NULL COMMENT '维护人姓名',
    start_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    cost DECIMAL(10,2) COMMENT '维护费用',
    result TINYINT NOT NULL DEFAULT 0 COMMENT '维护结果：0未完成 1已完成 2需返修',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备维护记录表';

ALTER TABLE t_equipment
    MODIFY status VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除';

ALTER TABLE t_maintenance
    MODIFY maintenance_content TEXT NULL,
    MODIFY maintenance_user_id BIGINT NULL,
    MODIFY maintenance_user_name VARCHAR(50) NULL,
    MODIFY start_time DATETIME NULL,
    MODIFY result VARCHAR(50) NULL,
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '维修状态',
    ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- t_bom（物料清单表）
CREATE TABLE IF NOT EXISTS t_bom (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'BOM ID，主键自增',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    material_name VARCHAR(100) NOT NULL COMMENT '物料名称',
    quantity DECIMAL(10,2) NOT NULL COMMENT '用量',
    unit VARCHAR(20) NOT NULL COMMENT '单位',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料清单表';

-- t_work_order（工单表）
CREATE TABLE IF NOT EXISTS t_work_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工单ID，主键自增',
    wo_no VARCHAR(50) NOT NULL UNIQUE COMMENT '工单编号，唯一',
    production_order_id BIGINT NOT NULL COMMENT '生产订单ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    planned_quantity DECIMAL(10,2) NOT NULL COMMENT '计划数量',
    actual_quantity DECIMAL(10,2) DEFAULT 0 COMMENT '实际数量',
    planned_start_date DATE COMMENT '计划开始日期',
    planned_end_date DATE COMMENT '计划结束日期',
    actual_start_date DATETIME COMMENT '实际开始时间',
    actual_end_date DATETIME COMMENT '实际结束时间',
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT '工单状态：PLANNED已计划 ASSIGNED已分配 IN_PROGRESS进行中 COMPLETED已完成 CANCELLED已取消',
    department_id BIGINT COMMENT '部门ID',
    assigned_user_id BIGINT COMMENT '分配的用户ID',
    assigned_user_name VARCHAR(50) COMMENT '分配的用户姓名',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- t_attendance（考勤记录表）
CREATE TABLE IF NOT EXISTS t_attendance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考勤ID，主键自增',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    user_name VARCHAR(50) NOT NULL COMMENT '用户姓名',
    attendance_date DATE NOT NULL COMMENT '考勤日期',
    check_in_time DATETIME COMMENT '签到时间',
    check_out_time DATETIME COMMENT '签退时间',
    work_hours DECIMAL(5,2) COMMENT '工作时长（小时）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0缺勤 1正常 2迟到 3早退 4请假',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录表';

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE t_attendance;
TRUNCATE TABLE t_bom;
TRUNCATE TABLE t_work_order;
TRUNCATE TABLE t_maintenance;
TRUNCATE TABLE t_equipment;
TRUNCATE TABLE t_qc_inspection_item;
TRUNCATE TABLE t_qc_inspection;
TRUNCATE TABLE t_quality_check;
TRUNCATE TABLE t_quality_standard;
TRUNCATE TABLE t_inventory_transaction;
TRUNCATE TABLE t_stock_out_item;
TRUNCATE TABLE t_stock_out;
TRUNCATE TABLE t_stock_in_item;
TRUNCATE TABLE t_stock_in;
TRUNCATE TABLE t_inventory_out;
TRUNCATE TABLE t_inventory_in;
TRUNCATE TABLE t_inventory;
TRUNCATE TABLE t_location;
TRUNCATE TABLE t_warehouse;
TRUNCATE TABLE t_material;
TRUNCATE TABLE t_purchase_order_item;
TRUNCATE TABLE t_purchase_order;
TRUNCATE TABLE t_supplier;
TRUNCATE TABLE t_production_report;
TRUNCATE TABLE t_production_task;
TRUNCATE TABLE t_production_plan;
TRUNCATE TABLE t_order_product;
TRUNCATE TABLE t_order;
TRUNCATE TABLE t_product;
TRUNCATE TABLE t_dict_item;
TRUNCATE TABLE t_dict;
TRUNCATE TABLE t_operation_log;
TRUNCATE TABLE t_login_log;
TRUNCATE TABLE t_user;
TRUNCATE TABLE t_department;
TRUNCATE TABLE t_role;
SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE t_login_log MODIFY COLUMN user_id BIGINT;
ALTER TABLE t_operation_log MODIFY COLUMN user_id BIGINT;

INSERT INTO t_role (id, role_code, role_name, role_desc, sort, status) VALUES
(1, '101', '生产经理', '负责生产计划制定、任务分配和进度跟踪', 1, 1),
(2, '102', '车间主任', '负责车间日常管理和生产调度', 2, 1),
(3, '103', '操作工', '负责生产线操作和产品加工', 3, 1),
(4, '104', '质检员', '负责产品质量检测和质量标准执行', 4, 1),
(5, '105', '仓管员', '负责物料出入库管理和库存盘点', 5, 1),
(6, '106', '采购专员', '负责原材料采购和供应商管理', 6, 1),
(7, '107', '销售专员', '负责客户订单对接和销售业务', 7, 1),
(8, '108', '设备维护员', '负责设备维护和保养', 8, 1),
(9, '109', 'CEO', '公司最高管理者，拥有系统全部权限', 9, 1),
(10, '110', 'CFO', '财务总监，负责公司利润、收入和支出管理', 10, 1);

INSERT INTO t_user (user_code, username, password, real_name, phone, email, gender, role_id, dept_id, status) VALUES
('109001', 'admin', '123456', '张三', '13800138001', 'zhangsan@mouse.com', 1, 9, 1, 1),
('101001', 'prod_manager', '123456', '李四', '13800138002', 'lisi@mouse.com', 1, 1, 2, 1),
('102001', 'workshop_manager', '123456', '王五', '13800138003', 'wangwu@mouse.com', 1, 2, 8, 1),
('103001', 'worker1', '123456', '赵六', '13800138004', 'zhaoliu@mouse.com', 1, 3, 8, 1),
('103002', 'worker2', '123456', '孙七', '13800138005', 'sunqi@mouse.com', 1, 3, 9, 1),
('104001', 'qc_manager', '123456', '周八', '13800138006', 'zhouba@mouse.com', 1, 4, 3, 1),
('105001', 'warehouse_manager', '123456', '吴九', '13800138007', 'wujiu@mouse.com', 1, 5, 4, 1),
('106001', 'purchase_manager', '123456', '郑十', '13800138008', 'zhengshi@mouse.com', 1, 6, 5, 1),
('107001', 'sales_manager', '123456', '陈十一', '13800138009', 'chenshiyi@mouse.com', 1, 7, 6, 1),
('108001', 'equipment_manager', '123456', '黄十二', '13800138010', 'huangshier@mouse.com', 1, 8, 7, 1),
('110001', 'cfo', '123456', '钱十三', '13800138011', 'qianshisan@mouse.com', 1, 10, 1, 1);
