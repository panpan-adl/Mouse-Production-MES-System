package com.mouse.mes.service;

import org.springframework.jdbc.core.JdbcTemplate;

/** Warehouse positions and lots must refer to the material being moved. */
public final class StockPositionValidator {
    private StockPositionValidator(){}
    public static void check(JdbcTemplate db,Long material,Long warehouse,Long location,Long lot){
        if(material==null||db.queryForObject("SELECT COUNT(*) FROM t_material WHERE id=? AND status=1",Integer.class,material)!=1)throw new IllegalArgumentException("物料不存在或未启用");
        if(location!=null&&db.queryForObject("SELECT COUNT(*) FROM t_location WHERE id=? AND warehouse_id=? AND is_deleted=0",Integer.class,location,warehouse)!=1)throw new IllegalArgumentException("库位不属于所选仓库");
        if(lot!=null&&db.queryForObject("SELECT COUNT(*) FROM t_lot WHERE id=? AND material_id=? AND is_deleted=0 AND (expire_date IS NULL OR expire_date>NOW())",Integer.class,lot,material)!=1)throw new IllegalArgumentException("批次与物料不匹配、已失效或已过期");
    }
}
