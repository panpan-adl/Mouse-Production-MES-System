package com.mouse.mes.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationConfig implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            addPurchasePriceColumn();
            updateStatusComment();
            updateRoleName();
        } catch (Exception e) {
            System.err.println("数据库迁移失败: " + e.getMessage());
        }
    }

    private void addPurchasePriceColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_equipment' AND COLUMN_NAME = 'purchase_price'",
                Integer.class
            );
            if (count != null && count == 0) {
                jdbcTemplate.execute(
                    "ALTER TABLE t_equipment ADD COLUMN purchase_price DECIMAL(12, 2) DEFAULT 0.00 COMMENT '购置价格' AFTER purchase_date"
                );
                System.out.println("已添加 t_equipment.purchase_price 字段");
            }
        } catch (Exception e) {
            System.err.println("添加 purchase_price 字段失败: " + e.getMessage());
        }
    }

    private void updateStatusComment() {
        try {
            jdbcTemplate.execute(
                "ALTER TABLE t_equipment MODIFY COLUMN status VARCHAR(30) NOT NULL DEFAULT 'NORMAL' " +
                "COMMENT '状态：NORMAL正常 REPAIR_REQUESTED报修中 REPAIRING维修中 REPAIRED已维修 SCRAPPED报废'"
            );
        } catch (Exception e) {
            System.err.println("更新 status 字段注释失败: " + e.getMessage());
        }
    }

    private void updateRoleName() {
        try {
            jdbcTemplate.execute(
                "UPDATE t_role SET role_name = '设备管理员', role_desc = '负责设备管理和维修' WHERE id = 8"
            );
            System.out.println("已更新角色名称为设备管理员");
        } catch (Exception e) {
            System.err.println("更新角色名称失败: " + e.getMessage());
        }
    }
}
