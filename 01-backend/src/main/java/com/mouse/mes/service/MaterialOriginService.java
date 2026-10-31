package com.mouse.mes.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Record an empty inventory origin only in the transaction that creates a brand new material. */
@Service
public class MaterialOriginService {
    private final JdbcTemplate db;
    public MaterialOriginService(JdbcTemplate db) { this.db=db; }
    public void created(long material) {
        if (db.queryForObject("SELECT COUNT(*) FROM t_inventory WHERE material_id=?",Integer.class,material)!=0
            || db.queryForObject("SELECT COUNT(*) FROM t_inventory_transaction WHERE material_id=?",Integer.class,material)!=0) {
            throw new IllegalArgumentException("已有库存历史的物料不能登记为空库存起点");
        }
        db.update("INSERT INTO mm_material_origin(material_id,created_at,ledger_id) SELECT ?,NOW(6),COALESCE(MAX(id),0) FROM t_inventory_transaction",material);
    }
}
