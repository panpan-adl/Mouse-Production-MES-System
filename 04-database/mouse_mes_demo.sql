/*
 Mouse MES 演示数据库初始化脚本
 MySQL 8.3
 说明：保留现有表字段结构，执行时删除并重建项目表，然后写入整洁的演示数据。
*/
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `mouse_mes`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE `mouse_mes`;

-- ----------------------------
-- Table structure for t_attendance
-- ----------------------------
DROP TABLE IF EXISTS `t_attendance`;
CREATE TABLE `t_attendance`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考勤ID，主键自增',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户姓名',
  `attendance_date` date NOT NULL COMMENT '考勤日期',
  `check_in_time` datetime NULL DEFAULT NULL COMMENT '签到时间',
  `check_out_time` datetime NULL DEFAULT NULL COMMENT '签退时间',
  `work_hours` decimal(5, 2) NULL DEFAULT NULL COMMENT '工作时长（小时）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0缺勤 1正常 2迟到 3早退 4请假',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考勤记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_bom
-- ----------------------------
DROP TABLE IF EXISTS `t_bom`;
CREATE TABLE `t_bom`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'BOM ID，主键自增',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `material_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '物料名称',
  `quantity` decimal(10, 2) NOT NULL COMMENT '用量',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '单位',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '物料清单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_bom_item
-- ----------------------------
DROP TABLE IF EXISTS `t_bom_item`;
CREATE TABLE `t_bom_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'BOM明细ID',
  `bom_id` bigint NOT NULL COMMENT 'BOM ID',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `quantity` decimal(10, 2) NOT NULL COMMENT '用量',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'BOM明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_department
-- ----------------------------
DROP TABLE IF EXISTS `t_department`;
CREATE TABLE `t_department`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID，主键自增',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父部门ID，顶级为0',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `dept_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门编码',
  `leader_id` bigint NULL DEFAULT NULL COMMENT '部门负责人ID',
  `leader_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '部门负责人姓名',
  `sort` int NOT NULL COMMENT '显示顺序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用，默认1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_dict
-- ----------------------------
DROP TABLE IF EXISTS `t_dict`;
CREATE TABLE `t_dict`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典ID',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典类型',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `dict_type`(`dict_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_dict_item
-- ----------------------------
DROP TABLE IF EXISTS `t_dict_item`;
CREATE TABLE `t_dict_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典数据ID',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典类型',
  `item_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典标签',
  `item_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典值',
  `sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_equipment
-- ----------------------------
DROP TABLE IF EXISTS `t_equipment`;
CREATE TABLE `t_equipment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备ID，主键自增',
  `equipment_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备编码，唯一',
  `equipment_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备名称',
  `equipment_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备类型',
  `model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备型号',
  `manufacturer` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生产厂商',
  `purchase_date` date NULL DEFAULT NULL COMMENT '购置日期',
  `warranty_period` int NULL DEFAULT NULL COMMENT '保修期（月）',
  `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备位置',
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NORMAL',
  `responsible_id` bigint NULL DEFAULT NULL COMMENT '负责人ID',
  `responsible_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '负责人姓名',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `equipment_code`(`equipment_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_inventory
-- ----------------------------
DROP TABLE IF EXISTS `t_inventory`;
CREATE TABLE `t_inventory`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_id` bigint NOT NULL,
  `location_id` bigint NULL DEFAULT NULL,
  `material_id` bigint NOT NULL,
  `lot_id` bigint NULL DEFAULT NULL,
  `quantity` decimal(10, 2) NOT NULL DEFAULT 0.00,
  `last_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_inventory_in
-- ----------------------------
DROP TABLE IF EXISTS `t_inventory_in`;
CREATE TABLE `t_inventory_in`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '入库单ID，主键自增',
  `in_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '入库单号，唯一',
  `in_type` tinyint NOT NULL COMMENT '入库类型：1采购入库 2生产入库 3退货入库',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `material_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '物料名称',
  `quantity` int NOT NULL COMMENT '入库数量',
  `unit_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '单价',
  `total_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '总金额',
  `supplier_id` bigint NULL DEFAULT NULL COMMENT '供应商ID（采购入库时）',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `in_date` date NOT NULL COMMENT '入库日期',
  `create_by` bigint NOT NULL COMMENT '操作人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `in_no`(`in_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '入库单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_inventory_out
-- ----------------------------
DROP TABLE IF EXISTS `t_inventory_out`;
CREATE TABLE `t_inventory_out`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '出库单ID，主键自增',
  `out_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '出库单号，唯一',
  `out_type` tinyint NOT NULL COMMENT '出库类型：1生产领料 2销售出库 3报废出库',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `material_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '物料名称',
  `quantity` int NOT NULL COMMENT '出库数量',
  `unit_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '单价',
  `total_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '总金额',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `out_date` date NOT NULL COMMENT '出库日期',
  `create_by` bigint NOT NULL COMMENT '操作人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `out_no`(`out_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '出库单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_inventory_transaction
-- ----------------------------
DROP TABLE IF EXISTS `t_inventory_transaction`;
CREATE TABLE `t_inventory_transaction`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transaction_no` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `transaction_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `business_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `source_order_id` bigint NULL DEFAULT NULL,
  `source_order_no` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `warehouse_id` bigint NOT NULL,
  `location_id` bigint NULL DEFAULT NULL,
  `material_id` bigint NOT NULL,
  `lot_id` bigint NULL DEFAULT NULL,
  `quantity` decimal(10, 2) NOT NULL,
  `before_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00,
  `after_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `transaction_no`(`transaction_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_location
-- ----------------------------
DROP TABLE IF EXISTS `t_location`;
CREATE TABLE `t_location`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '库位ID',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '库位编码',
  `location_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '库位名称',
  `location_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库位类型',
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '1' COMMENT '状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库位表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_login_log
-- ----------------------------
DROP TABLE IF EXISTS `t_login_log`;
CREATE TABLE `t_login_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID，主键自增',
  `user_id` bigint NULL DEFAULT NULL,
  `user_code` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户编码',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号',
  `login_type` tinyint NOT NULL COMMENT '操作类型：1登录 2登出',
  `login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录IP',
  `login_location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器类型',
  `os` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作系统',
  `status` tinyint NOT NULL COMMENT '登录状态：0失败 1成功',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '提示消息',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '登录日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_lot
-- ----------------------------
DROP TABLE IF EXISTS `t_lot`;
CREATE TABLE `t_lot`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `lot_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '批次号',
  `production_date` datetime NULL DEFAULT NULL COMMENT '生产日期',
  `expire_date` datetime NULL DEFAULT NULL COMMENT '过期日期',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `lot_no`(`lot_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '批次表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_maintenance
-- ----------------------------
DROP TABLE IF EXISTS `t_maintenance`;
CREATE TABLE `t_maintenance`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '维护ID，主键自增',
  `maintenance_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '维护单号，唯一',
  `equipment_id` bigint NOT NULL COMMENT '设备ID',
  `equipment_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备名称',
  `maintenance_type` tinyint NOT NULL COMMENT '维护类型：1保养 2维修 3巡检',
  `fault_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '故障描述（维修时）',
  `maintenance_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `maintenance_user_id` bigint NULL DEFAULT NULL,
  `maintenance_user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `start_time` datetime NULL DEFAULT NULL,
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `cost` decimal(10, 2) NULL DEFAULT NULL COMMENT '维护费用',
  `result` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `maintenance_no`(`maintenance_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备维护记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_material
-- ----------------------------
DROP TABLE IF EXISTS `t_material`;
CREATE TABLE `t_material`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '物料ID，主键自增',
  `material_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '物料编码，唯一',
  `material_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '物料名称',
  `material_type` tinyint NOT NULL COMMENT '物料类型：1原材料 2半成品 3成品',
  `specification` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '计量单位',
  `safety_stock` int NULL DEFAULT NULL COMMENT '安全库存',
  `current_stock` int NOT NULL DEFAULT 0 COMMENT '当前库存，默认0',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用，默认1',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `material_code`(`material_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '物料表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_material_issue
-- ----------------------------
DROP TABLE IF EXISTS `t_material_issue`;
CREATE TABLE `t_material_issue`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '领料单ID',
  `issue_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '领料单号',
  `work_order_id` bigint NOT NULL COMMENT '工单ID',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `total_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '总数量',
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `issue_no`(`issue_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '生产领料单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_material_issue_item
-- ----------------------------
DROP TABLE IF EXISTS `t_material_issue_item`;
CREATE TABLE `t_material_issue_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '领料明细ID',
  `issue_id` bigint NOT NULL COMMENT '领料单ID',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `lot_id` bigint NULL DEFAULT NULL COMMENT '批次ID',
  `planned_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '计划数量',
  `actual_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '实际数量',
  `location_id` bigint NULL DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '生产领料明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `t_operation_log`;
CREATE TABLE `t_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID，主键自增',
  `user_id` bigint NULL DEFAULT NULL,
  `user_code` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作人编码',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作人账号',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作模块',
  `operation` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作描述',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求URL',
  `request_param` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求参数',
  `response_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '返回结果',
  `operate_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作IP',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `status` tinyint NOT NULL COMMENT '操作状态：0失败 1成功',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误消息',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_order
-- ----------------------------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID，主键自增',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单编号，唯一',
  `customer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户名称',
  `customer_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户电话',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品名称',
  `quantity` int NOT NULL COMMENT '订单数量',
  `unit_price` decimal(10, 2) NOT NULL COMMENT '单价',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '总金额',
  `order_date` date NOT NULL COMMENT '下单日期',
  `delivery_date` date NULL DEFAULT NULL COMMENT '交付日期',
  `order_status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态：0待确认 1已确认 2生产中 3已完成 4已取消',
  `priority` tinyint NOT NULL DEFAULT 2 COMMENT '优先级：1低 2中 3高，默认2',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` bigint NOT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除，默认0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_order_product
-- ----------------------------
DROP TABLE IF EXISTS `t_order_product`;
CREATE TABLE `t_order_product`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单产品明细ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品名称',
  `quantity` int NOT NULL COMMENT '数量',
  `unit_price` decimal(10, 2) NOT NULL COMMENT '单价',
  `subtotal` decimal(10, 2) NOT NULL COMMENT '小计',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单产品明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_product
-- ----------------------------
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '产品ID，主键自增',
  `product_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品编码，唯一',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品名称',
  `product_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '产品类型',
  `model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '产品型号',
  `specification` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格参数',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '计量单位',
  `cost_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '成本价',
  `selling_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '销售价',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '产品描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0停售 1在售，默认1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `product_code`(`product_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '产品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_production_plan
-- ----------------------------
DROP TABLE IF EXISTS `t_production_plan`;
CREATE TABLE `t_production_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID，主键自增',
  `plan_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '计划编号，唯一',
  `order_id` bigint NULL DEFAULT NULL COMMENT '关联订单ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品名称',
  `plan_quantity` int NOT NULL COMMENT '计划数量',
  `completed_quantity` int NOT NULL DEFAULT 0 COMMENT '完成数量，默认0',
  `start_date` date NULL DEFAULT NULL COMMENT '计划开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '计划结束日期',
  `plan_status` tinyint NOT NULL DEFAULT 0 COMMENT '计划状态：0待开始 1进行中 2已完成 3已暂停',
  `priority` tinyint NOT NULL DEFAULT 2 COMMENT '优先级：1低 2中 3高，默认2',
  `create_by` bigint NOT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `plan_no`(`plan_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '生产计划表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_production_report
-- ----------------------------
DROP TABLE IF EXISTS `t_production_report`;
CREATE TABLE `t_production_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '报工ID，主键自增',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `report_user_id` bigint NOT NULL COMMENT '报工人ID',
  `report_user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '报工人姓名',
  `report_quantity` int NOT NULL COMMENT '本次完成数量',
  `qualified_quantity` int NOT NULL COMMENT '合格数量',
  `defect_quantity` int NOT NULL COMMENT '不合格数量',
  `report_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报工时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '生产报工表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_production_report_item
-- ----------------------------
DROP TABLE IF EXISTS `t_production_report_item`;
CREATE TABLE `t_production_report_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '报工明细ID',
  `report_id` bigint NOT NULL COMMENT '报工ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `good_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '良品数量',
  `defective_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '不良数量',
  `defect_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '缺陷类型',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '生产报工明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_production_task
-- ----------------------------
DROP TABLE IF EXISTS `t_production_task`;
CREATE TABLE `t_production_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID，主键自增',
  `task_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务编号，唯一',
  `plan_id` bigint NOT NULL COMMENT '生产计划ID',
  `task_type` tinyint NOT NULL COMMENT '任务类型：1加工 2组装',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品名称',
  `quantity` int NOT NULL COMMENT '任务数量',
  `assigned_user_id` bigint NULL DEFAULT NULL COMMENT '分配给的用户ID',
  `assigned_user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分配给的用户姓名',
  `task_status` tinyint NOT NULL DEFAULT 0 COMMENT '任务状态：0待领取 1进行中 2已完成 3已暂停',
  `start_time` datetime NULL DEFAULT NULL COMMENT '实际开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '实际结束时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `task_no`(`task_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '生产任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_purchase_order
-- ----------------------------
DROP TABLE IF EXISTS `t_purchase_order`;
CREATE TABLE `t_purchase_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '采购单ID，主键自增',
  `po_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '采购单编号，唯一',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `order_date` datetime NULL DEFAULT NULL COMMENT '采购日期',
  `expected_date` date NULL DEFAULT NULL COMMENT '预计到货日期',
  `total_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿 SUBMITTED已提交 APPROVED已审核 PARTIAL_RECEIVED部分到货 COMPLETED全部到货 CANCELLED已取消',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `po_no`(`po_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_purchase_order_item
-- ----------------------------
DROP TABLE IF EXISTS `t_purchase_order_item`;
CREATE TABLE `t_purchase_order_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID，主键自增',
  `po_id` bigint NOT NULL COMMENT '采购单ID',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `quantity` decimal(10, 2) NOT NULL COMMENT '采购数量',
  `unit_price` decimal(10, 2) NOT NULL COMMENT '单价',
  `amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '小计金额',
  `received_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '已到货数量',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购订单明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_qc_inspection
-- ----------------------------
DROP TABLE IF EXISTS `t_qc_inspection`;
CREATE TABLE `t_qc_inspection`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `inspection_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `inspection_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `source_order_id` bigint NULL DEFAULT NULL,
  `source_order_no` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sample_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00,
  `qualified_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00,
  `defective_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00,
  `qualified_rate` decimal(5, 2) NOT NULL DEFAULT 0.00,
  `result` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `defect_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `handle_method` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `business_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `inspection_no`(`inspection_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_qc_inspection_item
-- ----------------------------
DROP TABLE IF EXISTS `t_qc_inspection_item`;
CREATE TABLE `t_qc_inspection_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '质检明细ID',
  `inspection_id` bigint NOT NULL COMMENT '质检单ID',
  `material_id` bigint NULL DEFAULT NULL,
  `lot_id` bigint NULL DEFAULT NULL COMMENT '批次ID',
  `sample_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '抽检数量',
  `qualified_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '合格数量',
  `defective_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '不合格数量',
  `defect_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '缺陷类型',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `inspection_item` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '未命名检测项',
  `inspection_standard` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `actual_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `item_result` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `defect_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `handle_method` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '质检检验明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_quality_check
-- ----------------------------
DROP TABLE IF EXISTS `t_quality_check`;
CREATE TABLE `t_quality_check`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '质检ID，主键自增',
  `check_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '质检单号，唯一',
  `task_id` bigint NULL DEFAULT NULL COMMENT '关联任务ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品名称',
  `check_quantity` int NOT NULL COMMENT '质检数量',
  `qualified_quantity` int NOT NULL COMMENT '合格数量',
  `defect_quantity` int NOT NULL COMMENT '不合格数量',
  `qualified_rate` decimal(5, 2) NOT NULL COMMENT '合格率（%）',
  `check_result` tinyint NOT NULL COMMENT '质检结果：0不合格 1合格 2部分合格',
  `checker_id` bigint NOT NULL COMMENT '质检员ID',
  `checker_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '质检员姓名',
  `check_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '质检时间',
  `defect_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '缺陷描述',
  `handle_result` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理结果',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `check_no`(`check_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '质检记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_quality_standard
-- ----------------------------
DROP TABLE IF EXISTS `t_quality_standard`;
CREATE TABLE `t_quality_standard`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标准ID，主键自增',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品名称',
  `standard_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标准名称',
  `standard_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标准内容',
  `qualified_threshold` decimal(5, 2) NULL DEFAULT NULL COMMENT '合格阈值（%）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用，默认1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '质检标准表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '角色ID，主键自增',
  `role_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码（如101、109），唯一',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称（如生产经理、CEO）',
  `role_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色描述',
  `sort` int NOT NULL COMMENT '显示顺序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用，默认1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `role_code`(`role_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_sales_order
-- ----------------------------
DROP TABLE IF EXISTS `t_sales_order`;
CREATE TABLE `t_sales_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '销售订单ID',
  `so_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '销售订单编号',
  `customer_id` bigint NOT NULL COMMENT '客户ID',
  `order_date` datetime NULL DEFAULT NULL COMMENT '下单日期',
  `delivery_date` datetime NULL DEFAULT NULL COMMENT '交付日期',
  `total_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `so_no`(`so_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '销售订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_sales_order_item
-- ----------------------------
DROP TABLE IF EXISTS `t_sales_order_item`;
CREATE TABLE `t_sales_order_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '销售订单明细ID',
  `so_id` bigint NOT NULL COMMENT '销售订单ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `quantity` decimal(10, 2) NOT NULL COMMENT '数量',
  `unit_price` decimal(10, 2) NOT NULL COMMENT '单价',
  `amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '金额',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '销售订单明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_stock_in
-- ----------------------------
DROP TABLE IF EXISTS `t_stock_in`;
CREATE TABLE `t_stock_in`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '入库单ID，主键自增',
  `stock_in_no` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '入库单号，唯一',
  `stock_in_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '入库类型，例如 PURCHASE',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `source_order_id` bigint NULL DEFAULT NULL COMMENT '来源订单ID',
  `source_order_no` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '来源订单编号',
  `total_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '入库总数量',
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '入库状态',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `stock_in_no`(`stock_in_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通用入库单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_stock_in_item
-- ----------------------------
DROP TABLE IF EXISTS `t_stock_in_item`;
CREATE TABLE `t_stock_in_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '入库明细ID，主键自增',
  `stock_in_id` bigint NOT NULL COMMENT '入库单ID',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `lot_id` bigint NULL DEFAULT NULL COMMENT '批次ID',
  `quantity` decimal(10, 2) NOT NULL COMMENT '入库数量',
  `location_id` bigint NULL DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通用入库单明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_stock_out
-- ----------------------------
DROP TABLE IF EXISTS `t_stock_out`;
CREATE TABLE `t_stock_out`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stock_out_no` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `stock_out_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `warehouse_id` bigint NOT NULL,
  `source_order_id` bigint NULL DEFAULT NULL,
  `source_order_no` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `total_quantity` decimal(10, 2) NOT NULL DEFAULT 0.00,
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `stock_out_no`(`stock_out_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_stock_out_item
-- ----------------------------
DROP TABLE IF EXISTS `t_stock_out_item`;
CREATE TABLE `t_stock_out_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通用出库明细ID',
  `stock_out_id` bigint NOT NULL COMMENT '出库单ID',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `lot_id` bigint NULL DEFAULT NULL COMMENT '批次ID',
  `quantity` decimal(10, 2) NOT NULL COMMENT '数量',
  `location_id` bigint NULL DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通用出库明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_supplier
-- ----------------------------
DROP TABLE IF EXISTS `t_supplier`;
CREATE TABLE `t_supplier`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '供应商ID，主键自增',
  `supplier_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '供应商编码，唯一',
  `supplier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '供应商名称',
  `contact_person` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '开户银行',
  `bank_account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '银行账号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用，默认1',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `supplier_code`(`supplier_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '供应商表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键自增',
  `user_code` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '6位用户编码（前3位角色+后3位序号），唯一',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号，唯一',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码（加密存储）',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号码',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱地址',
  `gender` tinyint NULL DEFAULT NULL COMMENT '性别：0女 1男',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `role_id` int NOT NULL COMMENT '角色ID（关联t_role.id）',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0离职 1在职 2试用期，默认1',
  `entry_date` date NULL DEFAULT NULL COMMENT '入职日期',
  `leave_date` date NULL DEFAULT NULL COMMENT '离职日期',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除，默认0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_code`(`user_code` ASC) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_user_role
-- ----------------------------
DROP TABLE IF EXISTS `t_user_role`;
CREATE TABLE `t_user_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户角色关系ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_warehouse
-- ----------------------------
DROP TABLE IF EXISTS `t_warehouse`;
CREATE TABLE `t_warehouse`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '仓库ID，主键自增',
  `warehouse_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '仓库编码，唯一',
  `warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '仓库名称',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '仓库位置',
  `manager_id` bigint NULL DEFAULT NULL COMMENT '仓管员ID',
  `manager_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '仓管员姓名',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用，默认1',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `warehouse_code`(`warehouse_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '仓库表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_customer
-- ----------------------------
DROP TABLE IF EXISTS `t_customer`;
CREATE TABLE `t_customer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  `customer_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户编码',
  `customer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户名称',
  `contact_person` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '1' COMMENT '状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `customer_code`(`customer_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_production_order
-- ----------------------------
DROP TABLE IF EXISTS `t_production_order`;
CREATE TABLE `t_production_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '生产订单ID',
  `po_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '生产订单编号',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `planned_quantity` decimal(10, 2) NOT NULL COMMENT '计划数量',
  `actual_quantity` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '实际数量',
  `planned_start_date` date NULL DEFAULT NULL COMMENT '计划开始日期',
  `planned_end_date` date NULL DEFAULT NULL COMMENT '计划结束日期',
  `actual_start_date` datetime NULL DEFAULT NULL COMMENT '实际开始时间',
  `actual_end_date` datetime NULL DEFAULT NULL COMMENT '实际结束时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PLANNED' COMMENT '状态：PLANNED计划中 IN_PROGRESS生产中 COMPLETED已完成',
  `department_id` bigint NULL DEFAULT NULL COMMENT '部门ID',
  `created_by` bigint NOT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `po_no`(`po_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '生产订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_work_order
-- ----------------------------
DROP TABLE IF EXISTS `t_work_order`;
CREATE TABLE `t_work_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `wo_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工单编号',
  `production_order_id` bigint NOT NULL COMMENT '生产订单ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `planned_quantity` decimal(10, 2) NOT NULL COMMENT '计划数量',
  `actual_quantity` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '实际数量',
  `planned_start_date` date NULL DEFAULT NULL COMMENT '计划开始日期',
  `planned_end_date` date NULL DEFAULT NULL COMMENT '计划结束日期',
  `actual_start_date` datetime NULL DEFAULT NULL COMMENT '实际开始时间',
  `actual_end_date` datetime NULL DEFAULT NULL COMMENT '实际结束时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PLANNED' COMMENT '状态：PLANNED ASSIGNED IN_PROGRESS COMPLETED CANCELLED',
  `department_id` bigint NULL DEFAULT NULL COMMENT '部门ID',
  `assigned_user_id` bigint NULL DEFAULT NULL COMMENT '分配的用户ID',
  `assigned_user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分配的用户姓名',
  `created_by` bigint NOT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `wo_no`(`wo_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_uom
-- ----------------------------
DROP TABLE IF EXISTS `t_uom`;
CREATE TABLE `t_uom` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计量单位ID',
  `uom_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '单位编码',
  `uom_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '单位名称',
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '1' COMMENT '状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uom_code`(`uom_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '计量单位表' ROW_FORMAT = Dynamic;

-- ============================================================
-- 演示数据
-- 统一登录密码：123456
-- 主演示订单：ORD-20260715-001（待确认，500只A100有线办公鼠标）
-- ============================================================

-- 1. 角色
INSERT INTO `t_role` (`id`, `role_code`, `role_name`, `role_desc`, `sort`, `status`, `create_time`, `update_time`) VALUES
(1, '101', '生产经理', '负责生产计划制定、下发和进度跟踪', 1, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(2, '102', '车间主任', '负责车间调度和工单执行管理', 2, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(3, '103', '操作工', '负责领取生产任务并提交报工', 3, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(4, '104', '质检员', '负责来料、过程和成品质检', 4, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(5, '105', '仓管员', '负责库存、入库和出库业务', 5, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(6, '106', '采购专员', '负责采购订单和供应商管理', 6, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(7, '107', '销售专员', '负责客户和销售订单管理', 7, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(8, '108', '设备维护员', '负责设备台账和维修任务', 8, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(9, '109', 'CEO', '查看企业经营和生产全局数据', 9, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(10, '110', 'CFO', '查看销售收入、采购和维修支出', 10, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00');

-- 2. 部门
INSERT INTO `t_department` (`id`, `parent_id`, `dept_name`, `dept_code`, `leader_id`, `leader_name`, `sort`, `status`, `create_time`, `update_time`) VALUES
(1, 0, '总经办', 'CEO', 1, '张三', 1, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(2, 0, '生产部', 'PROD', 2, '李四', 2, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(3, 0, '质量部', 'QC', 6, '周八', 3, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(4, 0, '仓储部', 'WAREHOUSE', 7, '吴九', 4, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(5, 0, '采购部', 'PURCHASE', 8, '郑十', 5, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(6, 0, '销售部', 'SALES', 9, '陈十一', 6, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(7, 0, '设备部', 'EQUIPMENT', 10, '黄十二', 7, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(8, 0, '财务部', 'FINANCE', 11, '钱十三', 8, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(9, 0, '人力资源部', 'HR', NULL, NULL, 9, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00');

-- 3. 用户（登录密码均为明文123456，与当前登录代码保持一致）
INSERT INTO `t_user` (`id`, `user_code`, `username`, `password`, `real_name`, `phone`, `email`, `gender`, `avatar`, `role_id`, `dept_id`, `status`, `entry_date`, `leave_date`, `last_login_time`, `last_login_ip`, `create_time`, `update_time`, `deleted`) VALUES
(1, '109001', 'admin', '123456', '张三', '13800138001', 'zhangsan@mouse.com', 1, NULL, 9, 1, 1, '2023-03-01', NULL, '2026-07-15 08:20:00', '192.168.1.100', '2023-03-01 09:00:00', '2026-07-15 08:20:00', 0),
(2, '101001', 'prod_manager', '123456', '李四', '13800138002', 'lisi@mouse.com', 1, NULL, 1, 2, 1, '2023-04-10', NULL, '2026-07-15 08:25:00', '192.168.1.101', '2023-04-10 09:00:00', '2026-07-15 08:25:00', 0),
(3, '102001', 'workshop_manager', '123456', '王五', '13800138003', 'wangwu@mouse.com', 1, NULL, 2, 2, 1, '2023-05-08', NULL, '2026-07-15 08:27:00', '192.168.1.102', '2023-05-08 09:00:00', '2026-07-15 08:27:00', 0),
(4, '103001', 'worker1', '123456', '赵六', '13800138004', 'zhaoliu@mouse.com', 1, NULL, 3, 2, 1, '2024-02-18', NULL, '2026-07-15 08:31:00', '192.168.1.103', '2024-02-18 09:00:00', '2026-07-15 08:31:00', 0),
(5, '103002', 'worker2', '123456', '孙七', '13800138005', 'sunqi@mouse.com', 0, NULL, 3, 2, 1, '2024-02-20', NULL, '2026-07-15 08:33:00', '192.168.1.104', '2024-02-20 09:00:00', '2026-07-15 08:33:00', 0),
(6, '104001', 'qc_manager', '123456', '周八', '13800138006', 'zhouba@mouse.com', 0, NULL, 4, 3, 1, '2023-08-12', NULL, '2026-07-15 08:28:00', '192.168.1.105', '2023-08-12 09:00:00', '2026-07-15 08:28:00', 0),
(7, '105001', 'warehouse_manager', '123456', '吴九', '13800138007', 'wujiu@mouse.com', 1, NULL, 5, 4, 1, '2023-07-05', NULL, '2026-07-15 08:22:00', '192.168.1.106', '2023-07-05 09:00:00', '2026-07-15 08:22:00', 0),
(8, '106001', 'purchase_manager', '123456', '郑十', '13800138008', 'zhengshi@mouse.com', 1, NULL, 6, 5, 1, '2023-09-15', NULL, '2026-07-15 08:26:00', '192.168.1.107', '2023-09-15 09:00:00', '2026-07-15 08:26:00', 0),
(9, '107001', 'sales_manager', '123456', '陈十一', '13800138009', 'chenshiyi@mouse.com', 0, NULL, 7, 6, 1, '2023-06-11', NULL, '2026-07-15 08:24:00', '192.168.1.108', '2023-06-11 09:00:00', '2026-07-15 08:24:00', 0),
(10, '108001', 'equipment_manager', '123456', '黄十二', '13800138010', 'huangshier@mouse.com', 1, NULL, 8, 7, 1, '2023-11-03', NULL, '2026-07-15 08:29:00', '192.168.1.109', '2023-11-03 09:00:00', '2026-07-15 08:29:00', 0),
(11, '110001', 'cfo', '123456', '钱十三', '13800138011', 'qianshisan@mouse.com', 1, NULL, 10, 8, 1, '2023-03-06', NULL, '2026-07-15 08:21:00', '192.168.1.110', '2023-03-06 09:00:00', '2026-07-15 08:21:00', 0);

INSERT INTO `t_user_role` (`id`, `user_id`, `role_id`, `created_at`) VALUES
(1, 1, 9, '2023-03-01 09:00:00'),
(2, 2, 1, '2023-04-10 09:00:00'),
(3, 3, 2, '2023-05-08 09:00:00'),
(4, 4, 3, '2024-02-18 09:00:00'),
(5, 5, 3, '2024-02-20 09:00:00'),
(6, 6, 4, '2023-08-12 09:00:00'),
(7, 7, 5, '2023-07-05 09:00:00'),
(8, 8, 6, '2023-09-15 09:00:00'),
(9, 9, 7, '2023-06-11 09:00:00'),
(10, 10, 8, '2023-11-03 09:00:00'),
(11, 11, 10, '2023-03-06 09:00:00');

-- 4. 字典
INSERT INTO `t_dict` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `create_time`, `update_time`) VALUES
(1, '销售订单状态', 'order_status', 1, '销售订单流程状态', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(2, '生产任务状态', 'production_task_status', 1, '生产任务流程状态', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(3, '采购订单状态', 'purchase_order_status', 1, '采购订单流程状态', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(4, '质检结果', 'qc_result', 1, '质检判定结果', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(5, '设备状态', 'equipment_status', 1, '设备运行状态', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(6, '维修单状态', 'maintenance_status', 1, '设备维修流程状态', '2026-01-01 08:00:00', '2026-01-01 08:00:00');

INSERT INTO `t_dict_item` (`id`, `dict_type`, `item_label`, `item_value`, `sort`, `status`, `remark`, `create_time`, `update_time`) VALUES
(1, 'order_status', '待确认', '0', 1, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(2, 'order_status', '已确认', '1', 2, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(3, 'order_status', '生产中', '2', 3, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(4, 'order_status', '已完成', '3', 4, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(5, 'order_status', '已取消', '4', 5, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(6, 'production_task_status', '待领取', '0', 1, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(7, 'production_task_status', '已领取', '1', 2, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(8, 'production_task_status', '生产中', '2', 3, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(9, 'production_task_status', '待质检', '3', 4, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(10, 'production_task_status', '已完成', '4', 5, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(11, 'purchase_order_status', '草稿', 'DRAFT', 1, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(12, 'purchase_order_status', '已提交', 'SUBMITTED', 2, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(13, 'purchase_order_status', '已审核', 'APPROVED', 3, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(14, 'purchase_order_status', '部分到货', 'PARTIAL_RECEIVED', 4, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(15, 'purchase_order_status', '全部到货', 'COMPLETED', 5, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(16, 'purchase_order_status', '已取消', 'CANCELLED', 6, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(17, 'qc_result', '合格', 'QUALIFIED', 1, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(18, 'qc_result', '不合格', 'UNQUALIFIED', 2, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(19, 'qc_result', '让步接收', 'CONCESSION_ACCEPT', 3, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(20, 'qc_result', '返工', 'REWORK', 4, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(21, 'qc_result', '报废', 'SCRAP', 5, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(22, 'equipment_status', '正常', 'NORMAL', 1, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(23, 'equipment_status', '故障', 'FAULT', 2, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(24, 'equipment_status', '维修中', 'MAINTAINING', 3, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(25, 'equipment_status', '报废', 'SCRAPPED', 4, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(26, 'maintenance_status', '待维修', 'PENDING', 1, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(27, 'maintenance_status', '维修中', 'PROCESSING', 2, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(28, 'maintenance_status', '已完成', 'FINISHED', 3, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(29, 'maintenance_status', '已取消', 'CANCELLED', 4, 1, NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00');

-- 5. 客户、产品和计量单位
INSERT INTO `t_customer` (`id`, `customer_code`, `customer_name`, `contact_person`, `phone`, `email`, `address`, `status`, `created_at`, `updated_at`, `is_deleted`) VALUES
(1, 'CUS-001', '北京科创科技有限公司', '刘强', '13910001001', 'liuqiang@kechuang.com', '北京市朝阳区科技园路18号', '1', '2025-03-01 09:00:00', '2026-07-15 08:00:00', 0),
(2, 'CUS-002', '青岛智联办公设备有限公司', '李敏', '13910001002', 'limin@zhilian.com', '山东省青岛市崂山区科苑路66号', '1', '2025-04-16 09:00:00', '2026-07-15 08:00:00', 0),
(3, 'CUS-003', '上海星辰电子商务有限公司', '王磊', '13910001003', 'wanglei@xingchen.com', '上海市浦东新区张江路88号', '1', '2025-06-20 09:00:00', '2026-07-15 08:00:00', 0);

INSERT INTO `t_product` (`id`, `product_code`, `product_name`, `product_type`, `model`, `specification`, `unit`, `cost_price`, `selling_price`, `description`, `status`, `create_time`, `update_time`) VALUES
(1, 'PRD-MOUSE-A100', 'A100有线办公鼠标', '有线鼠标', 'A100', 'USB有线、1200DPI、3键', '只', 42.00, 89.00, '适用于办公场景的基础有线鼠标', 1, '2025-01-10 09:00:00', '2026-07-15 08:00:00'),
(2, 'PRD-MOUSE-G200', 'G200无线游戏鼠标', '无线鼠标', 'G200', '2.4G无线、3200DPI、6键', '只', 85.00, 159.00, '适用于游戏和高性能办公场景', 1, '2025-02-12 09:00:00', '2026-07-15 08:00:00'),
(3, 'PRD-MOUSE-S300', 'S300静音商务鼠标', '静音鼠标', 'S300', '静音按键、1600DPI、无线连接', '只', 63.00, 119.00, '适用于会议室和安静办公环境', 1, '2025-05-08 09:00:00', '2026-07-15 08:00:00');

INSERT INTO `t_uom` (`id`, `uom_code`, `uom_name`, `status`, `created_at`, `updated_at`, `is_deleted`) VALUES
(1, 'PCS', '只', '1', '2026-01-01 08:00:00', '2026-01-01 08:00:00', 0),
(2, 'EA', '个', '1', '2026-01-01 08:00:00', '2026-01-01 08:00:00', 0),
(3, 'BOARD', '块', '1', '2026-01-01 08:00:00', '2026-01-01 08:00:00', 0),
(4, 'SET', '套', '1', '2026-01-01 08:00:00', '2026-01-01 08:00:00', 0),
(5, 'CABLE', '根', '1', '2026-01-01 08:00:00', '2026-01-01 08:00:00', 0);

-- 6. 销售订单：第一条是答辩主演示订单
INSERT INTO `t_order` (`id`, `order_no`, `customer_name`, `customer_phone`, `product_name`, `quantity`, `unit_price`, `total_amount`, `order_date`, `delivery_date`, `order_status`, `priority`, `remark`, `create_by`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'ORD-20260715-001', '北京科创科技有限公司', '13910001001', 'A100有线办公鼠标', 500, 89.00, 44500.00, '2026-07-15', '2026-07-22', 0, 3, '主演示订单：销售专员确认后生成生产计划', 9, '2026-07-15 09:00:00', '2026-07-15 09:00:00', 0),
(2, 'ORD-20260712-002', '青岛智联办公设备有限公司', '13910001002', 'G200无线游戏鼠标', 300, 159.00, 47700.00, '2026-07-12', '2026-07-20', 2, 2, '生产中订单，用于展示任务领取和质检', 9, '2026-07-12 09:10:00', '2026-07-14 16:30:00', 0),
(3, 'ORD-20260701-003', '上海星辰电子商务有限公司', '13910001003', 'A100有线办公鼠标', 200, 89.00, 17800.00, '2026-07-01', '2026-07-10', 3, 2, '已完成订单，用于展示全流程历史记录', 9, '2026-07-01 10:00:00', '2026-07-09 16:00:00', 0),
(4, 'ORD-20260714-004', '北京科创科技有限公司', '13910001001', 'S300静音商务鼠标', 150, 119.00, 17850.00, '2026-07-14', '2026-07-21', 1, 1, '已确认待排产订单', 9, '2026-07-14 11:00:00', '2026-07-14 14:00:00', 0);

INSERT INTO `t_order_product` (`id`, `order_id`, `product_id`, `product_name`, `quantity`, `unit_price`, `subtotal`, `remark`) VALUES
(1, 1, 1, 'A100有线办公鼠标', 500, 89.00, 44500.00, '主演示订单明细'),
(2, 2, 2, 'G200无线游戏鼠标', 300, 159.00, 47700.00, NULL),
(3, 3, 1, 'A100有线办公鼠标', 200, 89.00, 17800.00, NULL),
(4, 4, 3, 'S300静音商务鼠标', 150, 119.00, 17850.00, NULL);

-- 财务看板使用的销售订单数据，包含近三个月趋势
INSERT INTO `t_sales_order` (`id`, `so_no`, `customer_id`, `order_date`, `delivery_date`, `total_amount`, `status`, `remark`, `created_by`, `created_at`, `updated_at`, `is_deleted`) VALUES
(1, 'SO-20260518-001', 1, '2026-05-18 09:00:00', '2026-05-25 18:00:00', 13350.00, 'COMPLETED', '5月历史销售订单', 9, '2026-05-18 09:00:00', '2026-05-25 18:00:00', 0),
(2, 'SO-20260610-001', 2, '2026-06-10 09:00:00', '2026-06-18 18:00:00', 31800.00, 'COMPLETED', '6月历史销售订单', 9, '2026-06-10 09:00:00', '2026-06-18 18:00:00', 0),
(3, 'SO-20260712-002', 2, '2026-07-12 09:10:00', '2026-07-20 18:00:00', 47700.00, 'IN_PRODUCTION', '对应生产中订单ORD-20260712-002', 9, '2026-07-12 09:10:00', '2026-07-14 16:30:00', 0),
(4, 'SO-20260701-003', 3, '2026-07-01 10:00:00', '2026-07-10 18:00:00', 17800.00, 'COMPLETED', '对应已完成订单ORD-20260701-003', 9, '2026-07-01 10:00:00', '2026-07-09 16:00:00', 0),
(5, 'SO-20260714-004', 1, '2026-07-14 11:00:00', '2026-07-21 18:00:00', 17850.00, 'CONFIRMED', '对应已确认订单ORD-20260714-004', 9, '2026-07-14 11:00:00', '2026-07-14 14:00:00', 0);

INSERT INTO `t_sales_order_item` (`id`, `so_id`, `product_id`, `quantity`, `unit_price`, `amount`, `remark`) VALUES
(1, 1, 1, 150.00, 89.00, 13350.00, NULL),
(2, 2, 2, 200.00, 159.00, 31800.00, NULL),
(3, 3, 2, 300.00, 159.00, 47700.00, NULL),
(4, 4, 1, 200.00, 89.00, 17800.00, NULL),
(5, 5, 3, 150.00, 119.00, 17850.00, NULL);

-- 7. 生产计划、任务、工单和报工
INSERT INTO `t_production_plan` (`id`, `plan_no`, `order_id`, `product_id`, `product_name`, `plan_quantity`, `completed_quantity`, `start_date`, `end_date`, `plan_status`, `priority`, `create_by`, `create_time`, `update_time`) VALUES
(1, 'PLAN-20260712-001', 2, 2, 'G200无线游戏鼠标', 300, 150, '2026-07-13', '2026-07-20', 1, 2, 2, '2026-07-12 14:00:00', '2026-07-14 16:30:00'),
(2, 'PLAN-20260701-002', 3, 1, 'A100有线办公鼠标', 200, 200, '2026-07-02', '2026-07-08', 2, 2, 2, '2026-07-01 15:00:00', '2026-07-08 16:00:00'),
(3, 'PLAN-20260714-003', 4, 3, 'S300静音商务鼠标', 150, 0, '2026-07-16', '2026-07-21', 0, 1, 2, '2026-07-14 15:00:00', '2026-07-14 15:00:00');

INSERT INTO `t_production_task` (`id`, `task_no`, `plan_id`, `task_type`, `product_id`, `product_name`, `quantity`, `assigned_user_id`, `assigned_user_name`, `task_status`, `start_time`, `end_time`, `remark`, `create_time`, `update_time`) VALUES
(1, 'TASK-20260712-001', 1, 2, 2, 'G200无线游戏鼠标', 150, 4, '赵六', 3, '2026-07-13 09:00:00', '2026-07-14 16:30:00', '已报工完成，等待质量检验', '2026-07-12 15:00:00', '2026-07-14 16:30:00'),
(2, 'TASK-20260712-002', 1, 2, 2, 'G200无线游戏鼠标', 150, NULL, NULL, 0, NULL, NULL, '待操作工领取的演示任务', '2026-07-12 15:05:00', '2026-07-12 15:05:00'),
(3, 'TASK-20260701-003', 2, 2, 1, 'A100有线办公鼠标', 200, 5, '孙七', 4, '2026-07-03 08:40:00', '2026-07-07 17:00:00', '历史已完成任务', '2026-07-02 09:00:00', '2026-07-08 16:00:00'),
(4, 'TASK-20260714-004', 3, 2, 3, 'S300静音商务鼠标', 150, 5, '孙七', 1, NULL, NULL, '已领取，尚未开始生产', '2026-07-14 15:10:00', '2026-07-15 08:30:00');

INSERT INTO `t_production_report` (`id`, `task_id`, `report_user_id`, `report_user_name`, `report_quantity`, `qualified_quantity`, `defect_quantity`, `report_time`, `remark`) VALUES
(1, 1, 4, '赵六', 150, 0, 0, '2026-07-14 16:30:00', '任务数量已全部报工，等待质检'),
(2, 3, 5, '孙七', 200, 198, 2, '2026-07-07 17:00:00', '历史报工记录，2只经返修后通过');

INSERT INTO `t_production_report_item` (`id`, `report_id`, `product_id`, `good_quantity`, `defective_quantity`, `defect_type`, `remark`) VALUES
(1, 1, 2, 150.00, 0.00, NULL, '等待质检判定'),
(2, 2, 1, 198.00, 2.00, '按键手感偏差', '返修后复检通过');

INSERT INTO `t_production_order` (`id`, `po_no`, `product_id`, `planned_quantity`, `actual_quantity`, `planned_start_date`, `planned_end_date`, `actual_start_date`, `actual_end_date`, `status`, `department_id`, `created_by`, `created_at`, `updated_at`, `is_deleted`) VALUES
(1, 'PROD-20260712-001', 2, 300.00, 150.00, '2026-07-13', '2026-07-20', '2026-07-13 09:00:00', NULL, 'IN_PROGRESS', 2, 2, '2026-07-12 14:00:00', '2026-07-14 16:30:00', 0),
(2, 'PROD-20260701-002', 1, 200.00, 200.00, '2026-07-02', '2026-07-08', '2026-07-03 08:40:00', '2026-07-07 17:00:00', 'COMPLETED', 2, 2, '2026-07-01 15:00:00', '2026-07-08 16:00:00', 0),
(3, 'PROD-20260714-003', 3, 150.00, 0.00, '2026-07-16', '2026-07-21', NULL, NULL, 'PLANNED', 2, 2, '2026-07-14 15:00:00', '2026-07-14 15:00:00', 0);

INSERT INTO `t_work_order` (`id`, `wo_no`, `production_order_id`, `product_id`, `planned_quantity`, `actual_quantity`, `planned_start_date`, `planned_end_date`, `actual_start_date`, `actual_end_date`, `status`, `department_id`, `assigned_user_id`, `assigned_user_name`, `created_by`, `created_at`, `updated_at`, `is_deleted`) VALUES
(1, 'WO-20260712-001', 1, 2, 150.00, 150.00, '2026-07-13', '2026-07-15', '2026-07-13 09:00:00', '2026-07-14 16:30:00', 'COMPLETED', 2, 4, '赵六', 2, '2026-07-12 15:00:00', '2026-07-14 16:30:00', 0),
(2, 'WO-20260712-002', 1, 2, 150.00, 0.00, '2026-07-15', '2026-07-18', NULL, NULL, 'PLANNED', 2, NULL, NULL, 2, '2026-07-12 15:05:00', '2026-07-12 15:05:00', 0),
(3, 'WO-20260701-003', 2, 1, 200.00, 200.00, '2026-07-03', '2026-07-07', '2026-07-03 08:40:00', '2026-07-07 17:00:00', 'COMPLETED', 2, 5, '孙七', 2, '2026-07-02 09:00:00', '2026-07-08 16:00:00', 0),
(4, 'WO-20260714-004', 3, 3, 150.00, 0.00, '2026-07-16', '2026-07-20', NULL, NULL, 'ASSIGNED', 2, 5, '孙七', 2, '2026-07-14 15:10:00', '2026-07-15 08:30:00', 0);

-- 8. 供应商、采购订单
INSERT INTO `t_supplier` (`id`, `supplier_code`, `supplier_name`, `contact_person`, `contact_phone`, `address`, `bank_name`, `bank_account`, `status`, `remark`, `create_time`, `update_time`) VALUES
(1, 'SUP-001', '青岛精密电子有限公司', '赵海', '13720001001', '山东省青岛市城阳区工业园12号', '中国工商银行青岛城阳支行', '622200001001', 1, '微动开关和编码器供应商', '2025-01-15 09:00:00', '2026-07-15 08:00:00'),
(2, 'SUP-002', '深圳芯联科技有限公司', '林峰', '13720001002', '广东省深圳市宝安区电子产业园8号', '中国建设银行深圳宝安支行', '622700001002', 1, 'PCB主板供应商', '2025-02-01 09:00:00', '2026-07-15 08:00:00'),
(3, 'SUP-003', '苏州瑞丰塑胶有限公司', '顾宁', '13720001003', '江苏省苏州市吴中区模具产业园5号', '中国银行苏州吴中支行', '621600001003', 1, '鼠标外壳供应商', '2025-02-18 09:00:00', '2026-07-15 08:00:00'),
(4, 'SUP-004', '宁波优线电子有限公司', '徐涛', '13720001004', '浙江省宁波市鄞州区线材园区3号', '中国农业银行宁波鄞州支行', '622800001004', 1, 'USB线材供应商', '2025-03-06 09:00:00', '2026-07-15 08:00:00');

INSERT INTO `t_purchase_order` (`id`, `po_no`, `supplier_id`, `order_date`, `expected_date`, `total_amount`, `status`, `remark`, `created_by`, `created_at`, `updated_at`, `is_deleted`) VALUES
(1, 'PO-20260715-001', 1, '2026-07-15 10:00:00', '2026-07-17', 600.00, 'APPROVED', '主演示补料：采购500个微动开关', 8, '2026-07-15 10:00:00', '2026-07-15 10:30:00', 0),
(2, 'PO-20260708-002', 2, '2026-07-08 09:30:00', '2026-07-10', 5400.00, 'COMPLETED', 'PCB主板采购，已全部到货', 8, '2026-07-08 09:30:00', '2026-07-10 14:00:00', 0),
(3, 'PO-20260710-003', 4, '2026-07-10 10:00:00', '2026-07-16', 700.00, 'PARTIAL_RECEIVED', 'USB连接线采购，已到货100根', 8, '2026-07-10 10:00:00', '2026-07-14 11:00:00', 0),
(4, 'PO-20260714-004', 3, '2026-07-14 15:30:00', '2026-07-18', 2400.00, 'DRAFT', 'A100上下盖采购草稿', 8, '2026-07-14 15:30:00', '2026-07-14 15:30:00', 0),
(5, 'PO-20260510-005', 3, '2026-05-10 09:00:00', '2026-05-15', 6000.00, 'COMPLETED', '5月历史塑胶件采购', 8, '2026-05-10 09:00:00', '2026-05-15 14:00:00', 0),
(6, 'PO-20260605-006', 2, '2026-06-05 09:00:00', '2026-06-10', 7200.00, 'COMPLETED', '6月历史PCB采购', 8, '2026-06-05 09:00:00', '2026-06-10 14:00:00', 0),
(7, 'PO-20260713-007', 1, '2026-07-13 14:00:00', '2026-07-16', 300.00, 'CANCELLED', '需求取消的编码器采购单', 8, '2026-07-13 14:00:00', '2026-07-13 16:00:00', 0);

-- 物料ID在后面的t_material中固定为1-10
INSERT INTO `t_purchase_order_item` (`id`, `po_id`, `material_id`, `quantity`, `unit_price`, `amount`, `received_quantity`, `remark`) VALUES
(1, 1, 4, 500.00, 1.20, 600.00, 0.00, '用于补足A100生产所需微动开关'),
(2, 2, 3, 300.00, 18.00, 5400.00, 300.00, NULL),
(3, 3, 6, 200.00, 3.50, 700.00, 100.00, NULL),
(4, 4, 1, 200.00, 6.00, 1200.00, 0.00, 'A100上盖'),
(5, 4, 2, 200.00, 6.00, 1200.00, 0.00, 'A100下盖'),
(6, 5, 1, 500.00, 6.00, 3000.00, 500.00, NULL),
(7, 5, 2, 500.00, 6.00, 3000.00, 500.00, NULL),
(8, 6, 3, 400.00, 18.00, 7200.00, 400.00, NULL),
(9, 7, 5, 100.00, 3.00, 300.00, 0.00, '订单已取消');

-- 9. 物料、仓库、库位、批次和库存
INSERT INTO `t_material` (`id`, `material_code`, `material_name`, `material_type`, `specification`, `unit`, `safety_stock`, `current_stock`, `status`, `remark`, `create_time`, `update_time`) VALUES
(1, 'MAT-SHELL-TOP-A100', 'A100鼠标上盖', 1, 'ABS黑色', '个', 300, 1200, 1, 'A100外壳上盖', '2025-01-10 09:00:00', '2026-07-15 08:00:00'),
(2, 'MAT-SHELL-BOTTOM-A100', 'A100鼠标下盖', 1, 'ABS黑色', '个', 300, 1200, 1, 'A100外壳下盖', '2025-01-10 09:00:00', '2026-07-15 08:00:00'),
(3, 'MAT-PCB-A100', 'A100 PCB主板', 1, 'USB主控板', '块', 500, 700, 1, 'A100核心电子组件', '2025-01-10 09:00:00', '2026-07-15 08:00:00'),
(4, 'MAT-SWITCH-01', '微动开关', 1, '寿命500万次', '个', 1200, 1050, 1, '当前低于安全库存，主演示补料物料', '2025-01-10 09:00:00', '2026-07-15 08:00:00'),
(5, 'MAT-ENCODER-01', '滚轮编码器', 1, '机械式编码器', '个', 500, 700, 1, '滚轮信号组件', '2025-01-10 09:00:00', '2026-07-15 08:00:00'),
(6, 'MAT-USB-CABLE-A100', 'USB连接线', 1, '1.5米黑色线材', '根', 500, 800, 1, 'A100连接线', '2025-01-10 09:00:00', '2026-07-15 08:00:00'),
(7, 'MAT-WHEEL-A100', '滚轮组件', 1, '橡胶防滑滚轮', '个', 500, 700, 1, 'A100滚轮组件', '2025-01-10 09:00:00', '2026-07-15 08:00:00'),
(8, 'MAT-FG-A100', 'A100有线办公鼠标成品', 3, 'A100成品', '只', 100, 200, 1, '成品库存', '2025-01-10 09:00:00', '2026-07-15 08:00:00'),
(9, 'MAT-FG-G200', 'G200无线游戏鼠标成品', 3, 'G200成品', '只', 100, 50, 1, '当前低于安全库存', '2025-02-12 09:00:00', '2026-07-15 08:00:00'),
(10, 'MAT-FG-S300', 'S300静音商务鼠标成品', 3, 'S300成品', '只', 100, 120, 1, '成品库存', '2025-05-08 09:00:00', '2026-07-15 08:00:00');

INSERT INTO `t_warehouse` (`id`, `warehouse_code`, `warehouse_name`, `location`, `manager_id`, `manager_name`, `status`, `remark`, `create_time`, `update_time`) VALUES
(1, 'WH-RM', '原材料仓', '1号厂房A区', 7, '吴九', 1, '存放塑胶件、电子件和线材', '2025-01-01 08:00:00', '2026-07-15 08:00:00'),
(2, 'WH-FG', '成品仓', '1号厂房B区', 7, '吴九', 1, '存放检验合格的鼠标成品', '2025-01-01 08:00:00', '2026-07-15 08:00:00'),
(3, 'WH-NG', '不合格品仓', '1号厂房C区', 7, '吴九', 1, '存放待处理不合格品', '2025-01-01 08:00:00', '2026-07-15 08:00:00');

INSERT INTO `t_location` (`id`, `warehouse_id`, `location_code`, `location_name`, `location_type`, `status`, `created_at`, `updated_at`, `is_deleted`) VALUES
(1, 1, 'RM-A01', '塑胶件库位', 'RAW_MATERIAL', '1', '2025-01-01 08:00:00', '2026-07-15 08:00:00', 0),
(2, 1, 'RM-A02', '电子件库位', 'RAW_MATERIAL', '1', '2025-01-01 08:00:00', '2026-07-15 08:00:00', 0),
(3, 1, 'RM-A03', '线材与滚轮库位', 'RAW_MATERIAL', '1', '2025-01-01 08:00:00', '2026-07-15 08:00:00', 0),
(4, 2, 'FG-B01', 'A100成品库位', 'FINISHED_GOODS', '1', '2025-01-01 08:00:00', '2026-07-15 08:00:00', 0),
(5, 2, 'FG-B02', 'G200和S300成品库位', 'FINISHED_GOODS', '1', '2025-01-01 08:00:00', '2026-07-15 08:00:00', 0),
(6, 3, 'NG-C01', '不合格品隔离库位', 'NONCONFORMING', '1', '2025-01-01 08:00:00', '2026-07-15 08:00:00', 0);

INSERT INTO `t_lot` (`id`, `material_id`, `lot_no`, `production_date`, `expire_date`, `created_at`, `updated_at`, `is_deleted`) VALUES
(1, 1, 'LOT-20260701-SHELL-TOP', '2026-07-01 08:00:00', NULL, '2026-07-01 08:00:00', '2026-07-01 08:00:00', 0),
(2, 2, 'LOT-20260701-SHELL-BOTTOM', '2026-07-01 08:00:00', NULL, '2026-07-01 08:00:00', '2026-07-01 08:00:00', 0),
(3, 3, 'LOT-20260710-PCB', '2026-07-08 08:00:00', NULL, '2026-07-10 14:00:00', '2026-07-10 14:00:00', 0),
(4, 4, 'LOT-20260701-SWITCH', '2026-07-01 08:00:00', NULL, '2026-07-01 08:00:00', '2026-07-01 08:00:00', 0),
(5, 5, 'LOT-20260701-ENCODER', '2026-07-01 08:00:00', NULL, '2026-07-01 08:00:00', '2026-07-01 08:00:00', 0),
(6, 6, 'LOT-20260714-USB', '2026-07-09 08:00:00', NULL, '2026-07-14 11:00:00', '2026-07-14 11:00:00', 0),
(7, 7, 'LOT-20260701-WHEEL', '2026-07-01 08:00:00', NULL, '2026-07-01 08:00:00', '2026-07-01 08:00:00', 0),
(8, 8, 'LOT-FG-A100-20260707', '2026-07-07 17:00:00', NULL, '2026-07-07 17:00:00', '2026-07-07 17:00:00', 0),
(9, 9, 'LOT-FG-G200-20260625', '2026-06-25 17:00:00', NULL, '2026-06-25 17:00:00', '2026-06-25 17:00:00', 0),
(10, 10, 'LOT-FG-S300-20260705', '2026-07-05 17:00:00', NULL, '2026-07-05 17:00:00', '2026-07-05 17:00:00', 0);

INSERT INTO `t_inventory` (`id`, `warehouse_id`, `location_id`, `material_id`, `lot_id`, `quantity`, `last_update_time`, `is_deleted`) VALUES
(1, 1, 1, 1, 1, 1200.00, '2026-07-15 08:00:00', 0),
(2, 1, 1, 2, 2, 1200.00, '2026-07-15 08:00:00', 0),
(3, 1, 2, 3, 3, 700.00, '2026-07-15 08:00:00', 0),
(4, 1, 2, 4, 4, 1050.00, '2026-07-15 08:00:00', 0),
(5, 1, 2, 5, 5, 700.00, '2026-07-15 08:00:00', 0),
(6, 1, 3, 6, 6, 800.00, '2026-07-15 08:00:00', 0),
(7, 1, 3, 7, 7, 700.00, '2026-07-15 08:00:00', 0),
(8, 2, 4, 8, 8, 200.00, '2026-07-15 08:00:00', 0),
(9, 2, 5, 9, 9, 50.00, '2026-07-15 08:00:00', 0),
(10, 2, 5, 10, 10, 120.00, '2026-07-15 08:00:00', 0);

-- 10. 入库、出库和库存流水
INSERT INTO `t_stock_in` (`id`, `stock_in_no`, `stock_in_type`, `warehouse_id`, `source_order_id`, `source_order_no`, `total_quantity`, `status`, `remark`, `created_by`, `created_at`, `updated_at`, `is_deleted`) VALUES
(1, 'SI-PO-20260708-002-001', 'PURCHASE', 1, 2, 'PO-20260708-002', 300.00, 'COMPLETED', 'PCB主板采购入库', 7, '2026-07-10 14:00:00', '2026-07-10 14:00:00', 0),
(2, 'SI-PO-20260710-003-001', 'PURCHASE', 1, 3, 'PO-20260710-003', 100.00, 'COMPLETED', 'USB连接线部分到货入库', 7, '2026-07-14 11:00:00', '2026-07-14 11:00:00', 0),
(3, 'SI-PROD-20260707-001', 'PRODUCTION', 2, 2, 'PROD-20260701-002', 400.00, 'COMPLETED', 'A100成品生产入库', 7, '2026-07-07 18:00:00', '2026-07-07 18:00:00', 0);

INSERT INTO `t_stock_in_item` (`id`, `stock_in_id`, `material_id`, `lot_id`, `quantity`, `location_id`, `remark`) VALUES
(1, 1, 3, 3, 300.00, 2, '采购入库'),
(2, 2, 6, 6, 100.00, 3, '部分到货入库'),
(3, 3, 8, 8, 400.00, 4, '成品入库');

INSERT INTO `t_stock_out` (`id`, `stock_out_no`, `stock_out_type`, `warehouse_id`, `source_order_id`, `source_order_no`, `total_quantity`, `status`, `remark`, `created_by`, `created_at`, `updated_at`, `is_deleted`) VALUES
(1, 'SOUT-SALES-20260709-001', 'SALES', 2, 3, 'ORD-20260701-003', 200.00, 'COMPLETED', 'A100历史订单销售出库', 7, '2026-07-09 15:00:00', '2026-07-09 15:00:00', 0),
(2, 'SOUT-SCRAP-20260713-002', 'SCRAP', 1, NULL, NULL, 20.00, 'COMPLETED', '损坏微动开关报废出库', 7, '2026-07-13 16:00:00', '2026-07-13 16:00:00', 0);

INSERT INTO `t_stock_out_item` (`id`, `stock_out_id`, `material_id`, `lot_id`, `quantity`, `location_id`, `remark`) VALUES
(1, 1, 8, 8, 200.00, 4, '按销售订单出库'),
(2, 2, 4, 4, 20.00, 2, '损坏物料报废');

INSERT INTO `t_inventory_transaction` (`id`, `transaction_no`, `transaction_type`, `business_type`, `source_order_id`, `source_order_no`, `warehouse_id`, `location_id`, `material_id`, `lot_id`, `quantity`, `before_quantity`, `after_quantity`, `created_by`, `created_at`, `remark`) VALUES
(1, 'INV-IN-20260710-001', 'IN', 'PURCHASE', 2, 'PO-20260708-002', 1, 2, 3, 3, 300.00, 400.00, 700.00, 7, '2026-07-10 14:00:00', 'PCB主板采购入库'),
(2, 'INV-IN-20260714-002', 'IN', 'PURCHASE', 3, 'PO-20260710-003', 1, 3, 6, 6, 100.00, 700.00, 800.00, 7, '2026-07-14 11:00:00', 'USB连接线部分到货'),
(3, 'INV-IN-20260707-003', 'IN', 'PRODUCTION', 2, 'PROD-20260701-002', 2, 4, 8, 8, 400.00, 0.00, 400.00, 7, '2026-07-07 18:00:00', 'A100成品入库'),
(4, 'INV-OUT-20260709-004', 'OUT', 'SALES', 3, 'ORD-20260701-003', 2, 4, 8, 8, 200.00, 400.00, 200.00, 7, '2026-07-09 15:00:00', 'A100销售出库'),
(5, 'INV-OUT-20260713-005', 'OUT', 'SCRAP', NULL, NULL, 1, 2, 4, 4, 20.00, 1070.00, 1050.00, 7, '2026-07-13 16:00:00', '损坏微动开关报废');

INSERT INTO `t_inventory_in` (`id`, `in_no`, `in_type`, `material_id`, `material_name`, `quantity`, `unit_price`, `total_amount`, `supplier_id`, `warehouse_id`, `in_date`, `create_by`, `create_time`, `remark`) VALUES
(1, 'IN-20260710-001', 1, 3, 'A100 PCB主板', 300, 18.00, 5400.00, 2, 1, '2026-07-10', 7, '2026-07-10 14:00:00', '采购入库'),
(2, 'IN-20260714-002', 1, 6, 'USB连接线', 100, 3.50, 350.00, 4, 1, '2026-07-14', 7, '2026-07-14 11:00:00', '部分到货入库'),
(3, 'IN-20260707-003', 2, 8, 'A100有线办公鼠标成品', 400, 42.00, 16800.00, NULL, 2, '2026-07-07', 7, '2026-07-07 18:00:00', '生产入库');

INSERT INTO `t_inventory_out` (`id`, `out_no`, `out_type`, `material_id`, `material_name`, `quantity`, `unit_price`, `total_amount`, `warehouse_id`, `out_date`, `create_by`, `create_time`, `remark`) VALUES
(1, 'OUT-20260709-001', 2, 8, 'A100有线办公鼠标成品', 200, 89.00, 17800.00, 2, '2026-07-09', 7, '2026-07-09 15:00:00', '销售出库'),
(2, 'OUT-20260713-002', 3, 4, '微动开关', 20, 1.20, 24.00, 1, '2026-07-13', 7, '2026-07-13 16:00:00', '报废出库');

-- 11. 生产领料
INSERT INTO `t_material_issue` (`id`, `issue_no`, `work_order_id`, `warehouse_id`, `total_quantity`, `status`, `remark`, `created_by`, `created_at`, `updated_at`, `is_deleted`) VALUES
(1, 'ISSUE-20260703-001', 3, 1, 1800.00, 'COMPLETED', 'A100历史生产工单领料', 3, '2026-07-03 08:10:00', '2026-07-03 08:30:00', 0);

INSERT INTO `t_material_issue_item` (`id`, `issue_id`, `material_id`, `lot_id`, `planned_quantity`, `actual_quantity`, `location_id`, `remark`) VALUES
(1, 1, 1, 1, 200.00, 200.00, 1, 'A100上盖'),
(2, 1, 2, 2, 200.00, 200.00, 1, 'A100下盖'),
(3, 1, 3, 3, 200.00, 200.00, 2, 'PCB主板'),
(4, 1, 4, 4, 600.00, 600.00, 2, '每只鼠标使用3个微动开关'),
(5, 1, 5, 5, 200.00, 200.00, 2, '滚轮编码器'),
(6, 1, 6, 6, 200.00, 200.00, 3, 'USB连接线'),
(7, 1, 7, 7, 200.00, 200.00, 3, '滚轮组件');

-- 12. BOM
INSERT INTO `t_bom` (`id`, `product_id`, `material_id`, `material_name`, `quantity`, `unit`, `remark`) VALUES
(1, 1, 1, 'A100鼠标上盖', 1.00, '个', '每只A100使用1个'),
(2, 1, 2, 'A100鼠标下盖', 1.00, '个', '每只A100使用1个'),
(3, 1, 3, 'A100 PCB主板', 1.00, '块', '每只A100使用1块'),
(4, 1, 4, '微动开关', 3.00, '个', '左右键及滚轮键共3个'),
(5, 1, 5, '滚轮编码器', 1.00, '个', '每只A100使用1个'),
(6, 1, 6, 'USB连接线', 1.00, '根', '每只A100使用1根'),
(7, 1, 7, '滚轮组件', 1.00, '个', '每只A100使用1个');

INSERT INTO `t_bom_item` (`id`, `bom_id`, `material_id`, `quantity`, `remark`) VALUES
(1, 1, 1, 1.00, 'A100上盖'),
(2, 2, 2, 1.00, 'A100下盖'),
(3, 3, 3, 1.00, 'A100 PCB主板'),
(4, 4, 4, 3.00, '微动开关'),
(5, 5, 5, 1.00, '滚轮编码器'),
(6, 6, 6, 1.00, 'USB连接线'),
(7, 7, 7, 1.00, '滚轮组件');

-- 13. 质量标准和质检记录
INSERT INTO `t_quality_standard` (`id`, `product_id`, `product_name`, `standard_name`, `standard_content`, `qualified_threshold`, `status`, `create_time`, `update_time`) VALUES
(1, 1, 'A100有线办公鼠标', 'A100外观与功能检验标准', '1.外壳无明显划痕和变形；2.左右键及滚轮按压正常；3.USB连接稳定；4.滚轮转动顺畅无卡顿。', 98.00, 1, '2026-01-15 09:00:00', '2026-07-15 08:00:00'),
(2, 1, 'A100有线办公鼠标', 'A100性能检验标准', '1.DPI误差不超过5%；2.按键寿命不少于500万次；3.工作电流不超过100mA；4.连接无丢帧。', 95.00, 1, '2026-01-15 09:00:00', '2026-07-15 08:00:00'),
(3, 2, 'G200无线游戏鼠标', 'G200成品检验标准', '1.无线连接稳定；2.六键功能正常；3.DPI档位切换准确；4.回报率达到产品规格。', 97.00, 1, '2026-02-20 09:00:00', '2026-07-15 08:00:00');

INSERT INTO `t_quality_check` (`id`, `check_no`, `task_id`, `product_id`, `product_name`, `check_quantity`, `qualified_quantity`, `defect_quantity`, `qualified_rate`, `check_result`, `checker_id`, `checker_name`, `check_date`, `defect_desc`, `handle_result`, `create_time`) VALUES
(1, 'QC-20260707-001', 3, 1, 'A100有线办公鼠标', 200, 198, 2, 99.00, 1, 6, '周八', '2026-07-07 17:30:00', '2只产品按键手感偏差', '返修后复检通过', '2026-07-07 17:30:00'),
(2, 'QC-20260711-002', NULL, 2, 'G200无线游戏鼠标', 100, 100, 0, 100.00, 1, 6, '周八', '2026-07-11 15:20:00', NULL, '检验合格', '2026-07-11 15:20:00');

INSERT INTO `t_qc_inspection` (`id`, `inspection_no`, `inspection_type`, `source_order_id`, `source_order_no`, `sample_quantity`, `qualified_quantity`, `defective_quantity`, `qualified_rate`, `result`, `defect_reason`, `handle_method`, `business_status`, `remark`, `created_by`, `created_at`, `updated_at`, `is_deleted`) VALUES
(1, 'QCI-IN-20260708-001', 'INCOMING', 2, 'PO-20260708-002', 50.00, 50.00, 0.00, 100.00, 'QUALIFIED', NULL, NULL, 'ALLOW_STOCK_IN', 'PCB主板来料质检合格', 6, '2026-07-10 13:00:00', '2026-07-10 13:00:00', 0),
(2, 'QCI-PROD-20260707-002', 'PRODUCTION', 3, 'WO-20260701-003', 200.00, 198.00, 2.00, 99.00, 'CONCESSION_ACCEPT', '2只产品按键手感轻微偏差', '现场调整后让步接收', 'ALLOW_NEXT_STEP', '生产过程检验', 6, '2026-07-07 16:20:00', '2026-07-07 16:40:00', 0),
(3, 'QCI-FG-20260707-003', 'FINISHED', 3, 'WO-20260701-003', 200.00, 200.00, 0.00, 100.00, 'QUALIFIED', NULL, NULL, 'ALLOW_FINISHED_STOCK_IN', 'A100成品质检合格', 6, '2026-07-07 17:20:00', '2026-07-07 17:20:00', 0),
(4, 'QCI-PROD-20260711-004', 'PRODUCTION', 1, 'WO-20260712-001', 20.00, 18.00, 2.00, 90.00, 'REWORK', '2只产品左键回弹不稳定', '退回生产线返工并复检', 'NEED_REWORK', '生产过程异常记录', 6, '2026-07-11 15:30:00', '2026-07-11 15:45:00', 0);

INSERT INTO `t_qc_inspection_item` (`id`, `inspection_id`, `material_id`, `lot_id`, `sample_quantity`, `qualified_quantity`, `defective_quantity`, `defect_type`, `remark`, `inspection_item`, `inspection_standard`, `actual_value`, `item_result`, `defect_reason`, `handle_method`) VALUES
(1, 1, 3, 3, 25.00, 25.00, 0.00, NULL, NULL, '外观检查', '焊点完整、板面无损伤', '25块均符合要求', 'QUALIFIED', NULL, NULL),
(2, 1, 3, 3, 25.00, 25.00, 0.00, NULL, NULL, '通电测试', '通电后功能正常', '25块均正常启动', 'QUALIFIED', NULL, NULL),
(3, 2, 8, 8, 100.00, 99.00, 1.00, '按键手感', '已现场调整', '左右键检查', '按压顺畅、回弹稳定', '1只手感偏差', 'CONCESSION_ACCEPT', '按键装配偏紧', '现场调整'),
(4, 2, 8, 8, 100.00, 99.00, 1.00, '滚轮手感', '已现场调整', '滚轮检查', '滚动顺畅无卡顿', '1只滚轮偏紧', 'CONCESSION_ACCEPT', '滚轮装配偏紧', '现场调整'),
(5, 3, 8, 8, 100.00, 100.00, 0.00, NULL, NULL, '外观终检', '外壳无划痕和变形', '全部合格', 'QUALIFIED', NULL, NULL),
(6, 3, 8, 8, 100.00, 100.00, 0.00, NULL, NULL, '功能终检', '按键、滚轮和USB功能正常', '全部合格', 'QUALIFIED', NULL, NULL),
(7, 4, 9, 9, 10.00, 9.00, 1.00, '左键回弹', '退回返工', '左键检查', '按压后应立即回弹', '1只回弹延迟', 'REWORK', '装配间隙不足', '重新装配后复检'),
(8, 4, 9, 9, 10.00, 9.00, 1.00, '左键回弹', '退回返工', '连续点击测试', '连续点击无卡键', '1只出现卡键', 'REWORK', '微动安装偏移', '调整微动位置后复检');

-- 14. 设备和维修
INSERT INTO `t_equipment` (`id`, `equipment_code`, `equipment_name`, `equipment_type`, `model`, `manufacturer`, `purchase_date`, `warranty_period`, `location`, `status`, `responsible_id`, `responsible_name`, `remark`, `create_time`, `update_time`, `is_deleted`) VALUES
(1, 'EQ-ASM-001', '自动组装机', '鼠标生产设备', 'ASM-800', '青岛智造设备有限公司', '2024-03-10', 36, '组装车间A线', 'NORMAL', 10, '黄十二', '用于鼠标自动组装', '2024-03-10 09:00:00', '2026-07-15 08:00:00', 0),
(2, 'EQ-TEST-001', '按键寿命测试机', '质检设备', 'KEY-500', '深圳精测仪器有限公司', '2024-05-18', 24, '质检室1区', 'NORMAL', 10, '黄十二', '用于按键寿命测试', '2024-05-18 09:00:00', '2026-07-15 08:00:00', 0),
(3, 'EQ-TEST-002', 'DPI性能测试台', '质检设备', 'DPI-3200', '深圳精测仪器有限公司', '2024-06-20', 24, '质检室1区', 'NORMAL', 10, '黄十二', '用于DPI和轨迹测试', '2024-06-20 09:00:00', '2026-07-15 08:00:00', 0),
(4, 'EQ-PACK-001', '自动包装机', '包装设备', 'PK-200', '苏州包装自动化有限公司', '2024-04-12', 36, '包装车间', 'NORMAL', 10, '黄十二', '用于成品包装', '2024-04-12 09:00:00', '2026-07-15 08:00:00', 0),
(5, 'EQ-SOLDER-001', 'PCB焊接工作站', '鼠标生产设备', 'SD-600', '东莞焊接科技有限公司', '2024-02-15', 24, '电子装配车间', 'MAINTAINING', 10, '黄十二', '温控模块正在维修', '2024-02-15 09:00:00', '2026-07-14 14:00:00', 0),
(6, 'EQ-INJECT-001', '外壳注塑机', '注塑设备', 'IM-120', '宁波塑机有限公司', '2023-08-16', 36, '注塑车间', 'FAULT', 10, '黄十二', '液压压力异常，等待维修', '2023-08-16 09:00:00', '2026-07-15 08:10:00', 0),
(7, 'EQ-OLD-001', '老旧注塑机', '注塑设备', 'IM-80', '宁波塑机有限公司', '2018-03-05', 12, '设备封存区', 'SCRAPPED', 10, '黄十二', '已达到报废年限', '2018-03-05 09:00:00', '2026-06-30 10:00:00', 0);

INSERT INTO `t_maintenance` (`id`, `maintenance_no`, `equipment_id`, `equipment_name`, `maintenance_type`, `fault_desc`, `maintenance_content`, `maintenance_user_id`, `maintenance_user_name`, `start_time`, `end_time`, `cost`, `result`, `remark`, `create_time`, `status`, `update_time`) VALUES
(1, 'MT-20260715-001', 6, '外壳注塑机', 2, '液压压力不稳定，设备无法正常启动', NULL, NULL, NULL, NULL, NULL, 0.00, NULL, '待设备维护员接单', '2026-07-15 08:10:00', 'PENDING', '2026-07-15 08:10:00'),
(2, 'MT-20260714-002', 5, 'PCB焊接工作站', 2, '温控模块温度波动较大', '检查温控传感器并更换继电器', 10, '黄十二', '2026-07-14 14:00:00', NULL, 0.00, NULL, '维修进行中', '2026-07-14 13:40:00', 'PROCESSING', '2026-07-14 14:00:00'),
(3, 'MT-20260705-003', 2, '按键寿命测试机', 1, NULL, '完成导轨清洁、传感器校准和润滑', 10, '黄十二', '2026-07-05 09:00:00', '2026-07-05 11:00:00', 120.00, 'NORMAL', '定期保养完成', '2026-07-05 08:50:00', 'FINISHED', '2026-07-05 11:00:00'),
(4, 'MT-20260701-004', 4, '自动包装机', 3, NULL, '完成安全巡检和皮带张力检查', 10, '黄十二', '2026-07-01 14:00:00', '2026-07-01 15:00:00', 80.00, 'NORMAL', '设备运行正常', '2026-07-01 13:50:00', 'FINISHED', '2026-07-01 15:00:00'),
(5, 'MT-20260612-005', 1, '自动组装机', 1, NULL, '完成机械臂润滑和定位校准', 10, '黄十二', '2026-06-12 09:00:00', '2026-06-12 11:30:00', 250.00, 'NORMAL', '6月定期保养', '2026-06-12 08:50:00', 'FINISHED', '2026-06-12 11:30:00'),
(6, 'MT-20260520-006', 3, 'DPI性能测试台', 2, '测试结果偶发漂移', '更换传感器并重新标定', 10, '黄十二', '2026-05-20 10:00:00', '2026-05-20 15:00:00', 300.00, 'REPAIRED', '5月历史维修记录', '2026-05-20 09:40:00', 'FINISHED', '2026-05-20 15:00:00');

-- 15. 考勤
INSERT INTO `t_attendance` (`id`, `user_id`, `user_name`, `attendance_date`, `check_in_time`, `check_out_time`, `work_hours`, `status`, `remark`, `create_time`) VALUES
(1, 1, '张三', '2026-07-15', '2026-07-15 08:20:00', '2026-07-15 17:30:00', 8.17, 1, NULL, '2026-07-15 08:20:00'),
(2, 2, '李四', '2026-07-15', '2026-07-15 08:25:00', '2026-07-15 17:35:00', 8.17, 1, NULL, '2026-07-15 08:25:00'),
(3, 3, '王五', '2026-07-15', '2026-07-15 08:27:00', '2026-07-15 17:32:00', 8.08, 1, NULL, '2026-07-15 08:27:00'),
(4, 4, '赵六', '2026-07-15', '2026-07-15 08:31:00', '2026-07-15 17:30:00', 7.98, 2, '迟到1分钟', '2026-07-15 08:31:00'),
(5, 5, '孙七', '2026-07-15', NULL, NULL, 0.00, 0, '未签到', '2026-07-15 09:00:00'),
(6, 6, '周八', '2026-07-15', '2026-07-15 08:28:00', '2026-07-15 17:35:00', 8.12, 1, NULL, '2026-07-15 08:28:00'),
(7, 7, '吴九', '2026-07-15', '2026-07-15 08:22:00', '2026-07-15 17:30:00', 8.13, 1, NULL, '2026-07-15 08:22:00'),
(8, 8, '郑十', '2026-07-15', '2026-07-15 08:26:00', '2026-07-15 17:31:00', 8.08, 1, NULL, '2026-07-15 08:26:00'),
(9, 9, '陈十一', '2026-07-15', '2026-07-15 08:24:00', '2026-07-15 17:34:00', 8.17, 1, NULL, '2026-07-15 08:24:00'),
(10, 10, '黄十二', '2026-07-15', '2026-07-15 08:29:00', '2026-07-15 17:40:00', 8.18, 1, NULL, '2026-07-15 08:29:00'),
(11, 11, '钱十三', '2026-07-15', NULL, NULL, 0.00, 4, '事假', '2026-07-15 08:00:00');

-- 16. 登录和操作日志
INSERT INTO `t_login_log` (`id`, `user_id`, `user_code`, `username`, `login_type`, `login_ip`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES
(1, 1, '109001', 'admin', 1, '192.168.1.100', '企业内网', 'Chrome 150', 'Windows 10', 1, '登录成功', '2026-07-15 08:20:00'),
(2, 2, '101001', 'prod_manager', 1, '192.168.1.101', '企业内网', 'Chrome 150', 'Windows 10', 1, '登录成功', '2026-07-15 08:25:00'),
(3, 9, '107001', 'sales_manager', 1, '192.168.1.108', '企业内网', 'Chrome 150', 'Windows 10', 1, '登录成功', '2026-07-15 08:24:00'),
(4, 7, '105001', 'warehouse_manager', 1, '192.168.1.106', '企业内网', 'Chrome 150', 'Windows 10', 1, '登录成功', '2026-07-15 08:22:00'),
(5, 8, '106001', 'purchase_manager', 1, '192.168.1.107', '企业内网', 'Chrome 150', 'Windows 10', 1, '登录成功', '2026-07-15 08:26:00'),
(6, 6, '104001', 'qc_manager', 1, '192.168.1.105', '企业内网', 'Chrome 150', 'Windows 10', 1, '登录成功', '2026-07-15 08:28:00');

INSERT INTO `t_operation_log` (`id`, `user_id`, `user_code`, `username`, `module`, `operation`, `method`, `request_url`, `request_param`, `response_result`, `operate_ip`, `operate_time`, `status`, `error_msg`) VALUES
(1, 9, '107001', 'sales_manager', '销售订单', '创建订单ORD-20260715-001', 'POST', '/api/orders', NULL, '创建成功', '192.168.1.108', '2026-07-15 09:00:00', 1, NULL),
(2, 2, '101001', 'prod_manager', '生产计划', '查看待排产订单', 'GET', '/api/orders/status/1', NULL, '查询成功', '192.168.1.101', '2026-07-15 09:10:00', 1, NULL),
(3, 8, '106001', 'purchase_manager', '采购订单', '审核采购单PO-20260715-001', 'PUT', '/api/purchase-orders/1/approve', NULL, '审核成功', '192.168.1.107', '2026-07-15 10:30:00', 1, NULL),
(4, 7, '105001', 'warehouse_manager', '库存管理', '查询低库存物料', 'GET', '/api/inventory/low-stock', NULL, '查询成功', '192.168.1.106', '2026-07-15 10:35:00', 1, NULL),
(5, 6, '104001', 'qc_manager', '质量管理', '查看待质检任务', 'GET', '/api/production-tasks', NULL, '查询成功', '192.168.1.105', '2026-07-15 10:40:00', 1, NULL),
(6, 10, '108001', 'equipment_manager', '设备维修', '查看待维修设备', 'GET', '/api/maintenances', NULL, '查询成功', '192.168.1.109', '2026-07-15 10:45:00', 1, NULL);

SET FOREIGN_KEY_CHECKS = 1;
