SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS mouse_mes;
CREATE DATABASE mouse_mes DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE mouse_mes;

CREATE TABLE t_role (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(10) NOT NULL UNIQUE COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_desc VARCHAR(255) COMMENT '角色描述',
    sort INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE t_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称',
    dept_code VARCHAR(50) NOT NULL COMMENT '部门编码',
    leader_id BIGINT COMMENT '负责人ID',
    leader_name VARCHAR(50) COMMENT '负责人姓名',
    sort INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：-1删除 0禁用 1活跃',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    user_code VARCHAR(6) NOT NULL UNIQUE COMMENT '6位用户编码',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '登录密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    gender TINYINT COMMENT '性别：0女 1男',
    avatar VARCHAR(255) COMMENT '头像',
    role_id INT NOT NULL COMMENT '角色ID',
    dept_id BIGINT COMMENT '部门ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0离职 1在职 2试用期',
    entry_date DATE COMMENT '入职日期',
    leave_date DATE COMMENT '离职日期',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE t_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户角色关系ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系表';

CREATE TABLE t_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '登录日志ID',
    user_id BIGINT COMMENT '用户ID',
    user_code VARCHAR(6) NOT NULL COMMENT '用户编码',
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    login_type TINYINT NOT NULL COMMENT '操作类型：1登录 2登出',
    login_ip VARCHAR(50) COMMENT '登录IP',
    login_location VARCHAR(100) COMMENT '登录地点',
    browser VARCHAR(255) COMMENT '浏览器',
    os VARCHAR(255) COMMENT '操作系统',
    status TINYINT NOT NULL COMMENT '状态：0失败 1成功',
    msg VARCHAR(255) COMMENT '提示消息',
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

CREATE TABLE t_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '操作日志ID',
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
    status TINYINT NOT NULL COMMENT '状态：0失败 1成功',
    error_msg TEXT COMMENT '错误消息'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

CREATE TABLE t_dict (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典ID',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_type VARCHAR(100) NOT NULL UNIQUE COMMENT '字典类型',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

CREATE TABLE t_dict_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典数据ID',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型',
    item_label VARCHAR(100) NOT NULL COMMENT '字典标签',
    item_value VARCHAR(100) NOT NULL COMMENT '字典值',
    sort INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

CREATE TABLE t_customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '客户ID',
    customer_code VARCHAR(50) NOT NULL UNIQUE COMMENT '客户编码',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    contact_person VARCHAR(50) COMMENT '联系人',
    phone VARCHAR(20) COMMENT '电话',
    email VARCHAR(100) COMMENT '邮箱',
    address VARCHAR(255) COMMENT '地址',
    status VARCHAR(30) NOT NULL DEFAULT '1' COMMENT '状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

CREATE TABLE t_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '产品ID',
    product_code VARCHAR(50) NOT NULL UNIQUE COMMENT '产品编码',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    product_type VARCHAR(50) COMMENT '产品类型',
    model VARCHAR(50) COMMENT '产品型号',
    specification VARCHAR(100) COMMENT '规格',
    unit VARCHAR(20) NOT NULL COMMENT '单位',
    cost_price DECIMAL(10,2) COMMENT '成本价',
    selling_price DECIMAL(10,2) COMMENT '销售价',
    description TEXT COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停售 1在售',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

CREATE TABLE t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单编号',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    customer_phone VARCHAR(20) COMMENT '客户电话',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    quantity INT NOT NULL COMMENT '订单数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    order_date DATE NOT NULL COMMENT '下单日期',
    delivery_date DATE COMMENT '交付日期',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态',
    priority TINYINT NOT NULL DEFAULT 2 COMMENT '优先级',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT NOT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE t_order_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单产品明细ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    quantity INT NOT NULL COMMENT '数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    subtotal DECIMAL(10,2) NOT NULL COMMENT '小计',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单产品明细表';

CREATE TABLE t_sales_order (
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

CREATE TABLE t_sales_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '销售订单明细ID',
    so_id BIGINT NOT NULL COMMENT '销售订单ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    quantity DECIMAL(10,2) NOT NULL COMMENT '数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '金额',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单明细表';

CREATE TABLE t_production_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '计划ID',
    plan_no VARCHAR(50) NOT NULL UNIQUE COMMENT '计划编号',
    order_id BIGINT COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    plan_quantity INT NOT NULL COMMENT '计划数量',
    completed_quantity INT NOT NULL DEFAULT 0 COMMENT '完成数量',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    plan_status TINYINT NOT NULL DEFAULT 0 COMMENT '计划状态',
    priority TINYINT NOT NULL DEFAULT 2 COMMENT '优先级',
    create_by BIGINT NOT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产计划表';

CREATE TABLE t_production_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    task_no VARCHAR(50) NOT NULL UNIQUE COMMENT '任务编号',
    plan_id BIGINT NOT NULL COMMENT '计划ID',
    task_type TINYINT NOT NULL COMMENT '任务类型',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    quantity INT NOT NULL COMMENT '任务数量',
    assigned_user_id BIGINT COMMENT '分配用户ID',
    assigned_user_name VARCHAR(50) COMMENT '分配用户姓名',
    task_status TINYINT NOT NULL DEFAULT 0 COMMENT '任务状态',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产任务表';

CREATE TABLE t_production_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '生产订单ID',
    po_no VARCHAR(50) NOT NULL UNIQUE COMMENT '生产订单编号',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    planned_quantity DECIMAL(10,2) NOT NULL COMMENT '计划数量',
    actual_quantity DECIMAL(10,2) DEFAULT 0 COMMENT '实际数量',
    planned_start_date DATETIME COMMENT '计划开始时间',
    planned_end_date DATETIME COMMENT '计划结束时间',
    actual_start_date DATETIME COMMENT '实际开始时间',
    actual_end_date DATETIME COMMENT '实际结束时间',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    department_id BIGINT COMMENT '部门ID',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产订单表';

CREATE TABLE t_work_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工单ID',
    wo_no VARCHAR(50) NOT NULL UNIQUE COMMENT '工单编号',
    production_order_id BIGINT NOT NULL COMMENT '生产订单ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    planned_quantity DECIMAL(10,2) NOT NULL COMMENT '计划数量',
    actual_quantity DECIMAL(10,2) DEFAULT 0 COMMENT '实际数量',
    planned_start_date DATETIME COMMENT '计划开始时间',
    planned_end_date DATETIME COMMENT '计划结束时间',
    actual_start_date DATETIME COMMENT '实际开始时间',
    actual_end_date DATETIME COMMENT '实际结束时间',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    department_id BIGINT COMMENT '部门ID',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

CREATE TABLE t_production_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报工ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    report_user_id BIGINT NOT NULL COMMENT '报工人ID',
    report_user_name VARCHAR(50) NOT NULL COMMENT '报工人姓名',
    report_quantity INT NOT NULL COMMENT '本次完成数量',
    qualified_quantity INT NOT NULL COMMENT '合格数量',
    defect_quantity INT NOT NULL COMMENT '不合格数量',
    report_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报工时间',
    remark VARCHAR(500) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产报工表';

CREATE TABLE t_production_report_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报工明细ID',
    report_id BIGINT NOT NULL COMMENT '报工ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    good_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '良品数量',
    defective_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '不良数量',
    defect_type VARCHAR(100) COMMENT '缺陷类型',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产报工明细表';

CREATE TABLE t_supplier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '供应商ID',
    supplier_code VARCHAR(50) NOT NULL UNIQUE COMMENT '供应商编码',
    supplier_name VARCHAR(100) NOT NULL COMMENT '供应商名称',
    contact_person VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    address VARCHAR(255) COMMENT '地址',
    bank_name VARCHAR(100) COMMENT '开户银行',
    bank_account VARCHAR(50) COMMENT '银行账号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：-1删除 0禁用 1活跃',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

CREATE TABLE t_purchase_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '采购订单ID',
    po_no VARCHAR(50) NOT NULL UNIQUE COMMENT '采购订单编号',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    order_date DATETIME COMMENT '采购日期',
    expected_date DATETIME COMMENT '预计到货日期',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '总金额',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/SUBMITTED/APPROVED/PARTIAL_RECEIVED/COMPLETED/CANCELLED',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单表';

CREATE TABLE t_purchase_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '采购订单明细ID',
    po_id BIGINT NOT NULL COMMENT '采购订单ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    quantity DECIMAL(10,2) NOT NULL COMMENT '采购数量',
    unit_price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '单价',
    amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '金额',
    received_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '已到货数量',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单明细表';

CREATE TABLE t_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '物料ID',
    material_code VARCHAR(50) NOT NULL UNIQUE COMMENT '物料编码',
    material_name VARCHAR(100) NOT NULL COMMENT '物料名称',
    material_type TINYINT NOT NULL COMMENT '物料类型：1原材料 2半成品 3成品',
    specification VARCHAR(100) COMMENT '规格型号',
    unit VARCHAR(20) NOT NULL COMMENT '计量单位',
    safety_stock INT COMMENT '安全库存',
    current_stock INT NOT NULL DEFAULT 0 COMMENT '当前库存',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料表';

CREATE TABLE t_warehouse (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '仓库ID',
    warehouse_code VARCHAR(50) NOT NULL UNIQUE COMMENT '仓库编码',
    warehouse_name VARCHAR(100) NOT NULL COMMENT '仓库名称',
    location VARCHAR(255) COMMENT '仓库位置',
    manager_id BIGINT COMMENT '仓管员ID',
    manager_name VARCHAR(50) COMMENT '仓管员姓名',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库表';

CREATE TABLE t_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库位ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    location_code VARCHAR(50) NOT NULL COMMENT '库位编码',
    location_name VARCHAR(100) NOT NULL COMMENT '库位名称',
    location_type VARCHAR(30) COMMENT '库位类型',
    status VARCHAR(30) NOT NULL DEFAULT '1' COMMENT '状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位表';

CREATE TABLE t_lot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '批次ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    lot_no VARCHAR(50) NOT NULL UNIQUE COMMENT '批次号',
    production_date DATETIME COMMENT '生产日期',
    expire_date DATETIME COMMENT '过期日期',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批次表';

CREATE TABLE t_inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    location_id BIGINT COMMENT '库位ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    lot_id BIGINT COMMENT '批次ID',
    quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '库存数量',
    last_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

CREATE TABLE t_inventory_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存流水ID',
    transaction_no VARCHAR(80) NOT NULL UNIQUE COMMENT '库存流水编号',
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

CREATE TABLE t_inventory_in (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '入库单ID',
    in_no VARCHAR(50) NOT NULL UNIQUE COMMENT '入库单号',
    in_type TINYINT NOT NULL COMMENT '入库类型',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    material_name VARCHAR(100) NOT NULL COMMENT '物料名称',
    quantity INT NOT NULL COMMENT '入库数量',
    unit_price DECIMAL(10,2) COMMENT '单价',
    total_amount DECIMAL(10,2) COMMENT '总金额',
    supplier_id BIGINT COMMENT '供应商ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    in_date DATE NOT NULL COMMENT '入库日期',
    create_by BIGINT NOT NULL COMMENT '操作人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    remark VARCHAR(500) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单表';

CREATE TABLE t_inventory_out (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '出库单ID',
    out_no VARCHAR(50) NOT NULL UNIQUE COMMENT '出库单号',
    out_type TINYINT NOT NULL COMMENT '出库类型',
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

CREATE TABLE t_stock_in (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通用入库单ID',
    stock_in_no VARCHAR(80) NOT NULL UNIQUE COMMENT '入库单号',
    stock_in_type VARCHAR(30) NOT NULL COMMENT '入库类型',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    source_order_id BIGINT COMMENT '来源订单ID',
    source_order_no VARCHAR(80) COMMENT '来源订单编号',
    total_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '总数量',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用入库单表';

CREATE TABLE t_stock_in_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通用入库明细ID',
    stock_in_id BIGINT NOT NULL COMMENT '入库单ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    lot_id BIGINT COMMENT '批次ID',
    quantity DECIMAL(10,2) NOT NULL COMMENT '数量',
    location_id BIGINT COMMENT '库位ID',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用入库明细表';

CREATE TABLE t_stock_out (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通用出库单ID',
    stock_out_no VARCHAR(80) NOT NULL UNIQUE COMMENT '出库单号',
    stock_out_type VARCHAR(30) NOT NULL COMMENT '出库类型',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    source_order_id BIGINT COMMENT '来源订单ID',
    source_order_no VARCHAR(80) COMMENT '来源订单编号',
    total_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '总数量',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用出库单表';

CREATE TABLE t_stock_out_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通用出库明细ID',
    stock_out_id BIGINT NOT NULL COMMENT '出库单ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    lot_id BIGINT COMMENT '批次ID',
    quantity DECIMAL(10,2) NOT NULL COMMENT '数量',
    location_id BIGINT COMMENT '库位ID',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用出库明细表';

CREATE TABLE t_material_issue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '领料单ID',
    issue_no VARCHAR(50) NOT NULL UNIQUE COMMENT '领料单号',
    work_order_id BIGINT NOT NULL COMMENT '工单ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    total_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '总数量',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产领料单表';

CREATE TABLE t_material_issue_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '领料明细ID',
    issue_id BIGINT NOT NULL COMMENT '领料单ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    lot_id BIGINT COMMENT '批次ID',
    planned_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '计划数量',
    actual_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '实际数量',
    location_id BIGINT COMMENT '库位ID',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产领料明细表';

CREATE TABLE t_quality_standard (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '质检标准ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    standard_name VARCHAR(100) NOT NULL COMMENT '标准名称',
    standard_content TEXT NOT NULL COMMENT '标准内容',
    qualified_threshold DECIMAL(5,2) COMMENT '合格阈值',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检标准表';

CREATE TABLE t_quality_check (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '质检记录ID',
    check_no VARCHAR(50) NOT NULL UNIQUE COMMENT '质检单号',
    task_id BIGINT COMMENT '任务ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    check_quantity INT NOT NULL COMMENT '质检数量',
    qualified_quantity INT NOT NULL COMMENT '合格数量',
    defect_quantity INT NOT NULL COMMENT '不合格数量',
    qualified_rate DECIMAL(5,2) NOT NULL COMMENT '合格率',
    check_result TINYINT NOT NULL COMMENT '质检结果',
    checker_id BIGINT NOT NULL COMMENT '质检员ID',
    checker_name VARCHAR(50) NOT NULL COMMENT '质检员姓名',
    check_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '质检时间',
    defect_desc TEXT COMMENT '缺陷描述',
    handle_result VARCHAR(500) COMMENT '处理结果',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检记录表';

CREATE TABLE t_qc_inspection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '质检单ID',
    inspection_no VARCHAR(50) NOT NULL UNIQUE COMMENT '质检单号',
    inspection_type VARCHAR(30) NOT NULL COMMENT '质检类型',
    source_order_id BIGINT COMMENT '来源单ID',
    source_order_no VARCHAR(80) COMMENT '来源单号',
    sample_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '抽检数量',
    qualified_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '合格数量',
    defective_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '不合格数量',
    result VARCHAR(30) COMMENT '质检结果',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检检验单表';

CREATE TABLE t_qc_inspection_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '质检明细ID',
    inspection_id BIGINT NOT NULL COMMENT '质检单ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    lot_id BIGINT COMMENT '批次ID',
    sample_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '抽检数量',
    qualified_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '合格数量',
    defective_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '不合格数量',
    defect_type VARCHAR(100) COMMENT '缺陷类型',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检检验明细表';

ALTER TABLE t_qc_inspection
    ADD COLUMN qualified_rate DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '合格率',
    ADD COLUMN defect_reason VARCHAR(500) COMMENT '不合格原因',
    ADD COLUMN handle_method VARCHAR(500) COMMENT '处理方式',
    ADD COLUMN business_status VARCHAR(50) COMMENT '后续业务状态';

ALTER TABLE t_qc_inspection_item
    ADD COLUMN inspection_item VARCHAR(100) NOT NULL COMMENT '检测项',
    ADD COLUMN inspection_standard VARCHAR(500) COMMENT '检测标准',
    ADD COLUMN actual_value VARCHAR(500) COMMENT '实测值',
    ADD COLUMN item_result VARCHAR(30) COMMENT '明细结果',
    ADD COLUMN defect_reason VARCHAR(500) COMMENT '不合格原因',
    ADD COLUMN handle_method VARCHAR(500) COMMENT '处理方式';

CREATE TABLE t_equipment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设备ID',
    equipment_code VARCHAR(50) NOT NULL UNIQUE COMMENT '设备编码',
    equipment_name VARCHAR(100) NOT NULL COMMENT '设备名称',
    equipment_type VARCHAR(50) COMMENT '设备类型',
    model VARCHAR(50) COMMENT '型号',
    manufacturer VARCHAR(100) COMMENT '厂商',
    purchase_date DATE COMMENT '购置日期',
    warranty_period INT COMMENT '保修期',
    location VARCHAR(100) COMMENT '设备位置',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0报废 1正常 2维修中 3停用',
    responsible_id BIGINT COMMENT '负责人ID',
    responsible_name VARCHAR(50) COMMENT '负责人姓名',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

CREATE TABLE t_maintenance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '维护记录ID',
    maintenance_no VARCHAR(50) NOT NULL UNIQUE COMMENT '维护单号',
    equipment_id BIGINT NOT NULL COMMENT '设备ID',
    equipment_name VARCHAR(100) NOT NULL COMMENT '设备名称',
    maintenance_type TINYINT NOT NULL COMMENT '维护类型',
    fault_desc TEXT COMMENT '故障描述',
    maintenance_content TEXT NOT NULL COMMENT '维护内容',
    maintenance_user_id BIGINT NOT NULL COMMENT '维护人ID',
    maintenance_user_name VARCHAR(50) NOT NULL COMMENT '维护人姓名',
    start_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    cost DECIMAL(10,2) COMMENT '维护费用',
    result TINYINT NOT NULL DEFAULT 0 COMMENT '结果',
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

CREATE TABLE t_bom (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'BOM ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    material_name VARCHAR(100) NOT NULL COMMENT '物料名称',
    quantity DECIMAL(10,2) NOT NULL COMMENT '用量',
    unit VARCHAR(20) NOT NULL COMMENT '单位',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM表';

CREATE TABLE t_bom_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'BOM明细ID',
    bom_id BIGINT NOT NULL COMMENT 'BOM ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    quantity DECIMAL(10,2) NOT NULL COMMENT '用量',
    remark VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM明细表';

CREATE TABLE t_uom (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '计量单位ID',
    uom_code VARCHAR(50) NOT NULL UNIQUE COMMENT '单位编码',
    uom_name VARCHAR(100) NOT NULL COMMENT '单位名称',
    status VARCHAR(30) NOT NULL DEFAULT '1' COMMENT '状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计量单位表';

CREATE TABLE t_attendance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考勤ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    user_name VARCHAR(50) NOT NULL COMMENT '用户姓名',
    attendance_date DATE NOT NULL COMMENT '考勤日期',
    check_in_time DATETIME COMMENT '签到时间',
    check_out_time DATETIME COMMENT '签退时间',
    work_hours DECIMAL(5,2) COMMENT '工时',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录表';

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

INSERT INTO t_department (id, parent_id, dept_name, dept_code, leader_id, leader_name, sort, status) VALUES
(1, 0, '总经办', 'CEO', NULL, NULL, 1, 1),
(2, 0, '生产部', 'PROD', NULL, NULL, 2, 1),
(3, 0, '质检部', 'QC', NULL, NULL, 3, 1),
(4, 0, '仓储部', 'WAREHOUSE', NULL, NULL, 4, 1),
(5, 0, '采购部', 'PURCHASE', NULL, NULL, 5, 1),
(6, 0, '销售部', 'SALES', NULL, NULL, 6, 1),
(7, 0, '设备部', 'EQUIPMENT', NULL, NULL, 7, 1),
(8, 0, '财务部', 'FINANCE', NULL, NULL, 8, 1),
(9, 0, '人力资源部', 'HR', NULL, NULL, 9, 1);

INSERT INTO t_user (user_code, username, password, real_name, phone, email, gender, role_id, dept_id, status) VALUES
('109001', 'admin', '123456', '张三', '13800138001', 'zhangsan@mouse.com', 1, 9, 1, 1),
('101001', 'prod_manager', '123456', '李四', '13800138002', 'lisi@mouse.com', 1, 1, 2, 1),
('102001', 'workshop_manager', '123456', '王五', '13800138003', 'wangwu@mouse.com', 1, 2, 2, 1),
('103001', 'worker1', '123456', '赵六', '13800138004', 'zhaoliu@mouse.com', 1, 3, 2, 1),
('104001', 'qc_manager', '123456', '周八', '13800138006', 'zhouba@mouse.com', 1, 4, 3, 1),
('105001', 'warehouse_manager', '123456', '吴九', '13800138007', 'wujiu@mouse.com', 1, 5, 4, 1),
('106001', 'purchase_manager', '123456', '郑十', '13800138008', 'zhengshi@mouse.com', 1, 6, 5, 1),
('107001', 'sales_manager', '123456', '陈十一', '13800138009', 'chenshiyi@mouse.com', 1, 7, 6, 1),
('108001', 'equipment_manager', '123456', '黄十二', '13800138010', 'huangshier@mouse.com', 1, 8, 7, 1),
('110001', 'cfo', '123456', '钱十三', '13800138011', 'qianshisan@mouse.com', 1, 10, 8, 1);

INSERT INTO t_warehouse (id, warehouse_code, warehouse_name, location, status, remark) VALUES
(1, 'WH-001', '默认仓库', '默认库区', 1, '系统初始化仓库');

INSERT INTO t_location (id, warehouse_id, location_code, location_name, location_type, status, is_deleted) VALUES
(1, 1, 'LOC-001', '默认库位', 'NORMAL', '1', 0);

INSERT INTO t_material (id, material_code, material_name, material_type, specification, unit, safety_stock, current_stock, status, remark) VALUES
(1, 'MAT-001', '测试物料', 1, '默认规格', '个', 10, 100, 1, '系统初始化物料');

INSERT INTO t_product (id, product_code, product_name, product_type, model, specification, unit, cost_price, selling_price, status) VALUES
(1, 'PROD-001', '测试鼠标', '鼠标', 'M-001', '默认规格', '个', 20.00, 59.00, 1);

SET FOREIGN_KEY_CHECKS = 1;
