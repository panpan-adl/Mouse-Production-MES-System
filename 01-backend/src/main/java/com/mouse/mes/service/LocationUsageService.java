package com.mouse.mes.service;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Shared deletion protection for both location management APIs. Call inside a transaction. */
@Service
public class LocationUsageService {
    private static final List<String> REFERENCE_TABLES = List.of(
        "t_inventory", "t_inventory_transaction", "t_stock_in_item", "t_stock_out_item",
        "t_material_issue_item", "mm_document_line", "mm_stock_posting");
    private final JdbcTemplate db;
    private final FulfillmentService fulfillment;

    public LocationUsageService(JdbcTemplate db, FulfillmentService fulfillment) {
        this.db = db;
        this.fulfillment = fulfillment;
    }

    public void requireUnused(long id) {
        fulfillment.lock();
        if (db.queryForList("SELECT id FROM t_location WHERE id=? AND is_deleted=0 FOR UPDATE", id).isEmpty()) {
            throw new IllegalArgumentException("库位不存在");
        }
        // Include zero balances and historical/deleted documents: their trace still references this location.
        for (String table : REFERENCE_TABLES) {
            Long count = db.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE location_id=?", Long.class, id);
            if (count != null && count > 0) {
                throw new IllegalArgumentException("库位已有库存、业务单据或历史流水引用，不能删除，请将状态改为停用");
            }
        }
    }

    public void retireFoundationMirror(long foundationId) {
        // The configuration and warehouse tables have independent IDs; match by warehouse and code.
        var physical = db.queryForList("SELECT t.id FROM t_location t JOIN mm_area a ON a.warehouse_id=t.warehouse_id "
            + "JOIN mm_location l ON l.area_id=a.id AND l.code=t.location_code WHERE l.id=?", foundationId);
        for (var row : physical) {
            long id = ((Number) row.get("id")).longValue();
            requireUnused(id);
            db.update("UPDATE t_location SET is_deleted=1 WHERE id=?", id);
        }
    }

    public void syncFoundationStatus(long physicalId) {
        db.update("UPDATE mm_location l JOIN mm_area a ON a.id=l.area_id JOIN t_location t ON t.warehouse_id=a.warehouse_id AND t.location_code=l.code SET l.status=IF(t.status='1',1,0) WHERE t.id=?",physicalId);
    }

    public void removeFoundationMirror(long physicalId) {
        db.update("DELETE l FROM mm_location l JOIN mm_area a ON a.id=l.area_id "
            + "JOIN t_location t ON t.warehouse_id=a.warehouse_id AND t.location_code=l.code WHERE t.id=?", physicalId);
    }
}
