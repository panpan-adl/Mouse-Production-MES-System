package com.mouse.mes.service;

import java.math.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Work-order/lot subledger: raw warehouse issues, report consumption/output and transfers. */
@Service
public class LineStockService {
    @org.springframework.beans.factory.annotation.Autowired private InventoryService inventory;
    private final JdbcTemplate db;
    public LineStockService(JdbcTemplate db){this.db=db;}
    static long id(Object v){return MesRulesService.id(v);}static BigDecimal dec(Object v){return MesRulesService.dec(v);}static void require(boolean b,String m){MesRulesService.require(b,m);}
    public void snapshot(long wo,List<Map<String,Object>> operations,long rootMaterial,BigDecimal quantity){
        var configured=db.queryForList("SELECT c.* FROM mm_task_consumption c JOIN mm_operation o ON o.id=c.operation_id WHERE o.route_id=?",operations.get(0).get("route_id"));
        if(configured.isEmpty()){
            for(var m:db.queryForList("SELECT * FROM mm_work_material WHERE work_order_id=?",wo))db.update("INSERT INTO mm_work_consumption(work_order_id,operation_id,material_id,quantity_per_unit) VALUES(?,?,?,?)",wo,operations.get(0).get("id"),m.get("material_id"),dec(m.get("required_quantity")).divide(quantity,6,RoundingMode.UNNECESSARY));
        }else{
            for(var c:configured)db.update("INSERT INTO mm_work_consumption(work_order_id,operation_id,material_id,quantity_per_unit) VALUES(?,?,?,?)",wo,c.get("operation_id"),c.get("material_id"),c.get("quantity_per_unit"));
            for(var m:db.queryForList("SELECT * FROM mm_work_material WHERE work_order_id=?",wo)){
                BigDecimal per=db.queryForObject("SELECT COALESCE(SUM(quantity_per_unit),0) FROM mm_work_consumption WHERE work_order_id=? AND material_id=?",BigDecimal.class,wo,m.get("material_id"));
                require(per.multiply(quantity).compareTo(dec(m.get("required_quantity")))==0,"工序原材料消耗总量必须与BOM需求一致："+m.get("material_code"));
            }
        }
        for(int i=0;i<operations.size();i++){var o=operations.get(i);Long output=null;if(id(o.get("output_material_id"))>0)output=id(o.get("output_material_id"));else if(i==operations.size()-1)output=rootMaterial;db.update("INSERT INTO mm_work_output(work_order_id,operation_id,material_id,is_key) VALUES(?,?,?,?)",wo,o.get("id"),output,o.get("is_key"));}
    }
    public long outputLot(long wo,long material){
        String lotNo="FG-MO-"+wo;
        var product=db.queryForList("SELECT material_id FROM mm_work_product WHERE work_order_id=?",wo);
        if(product.isEmpty()||id(product.get(0).get("material_id"))!=material)lotNo="WIP-MO-"+wo+"-"+material;
        var existing=db.queryForList("SELECT id FROM t_lot WHERE lot_no=? AND material_id=?",lotNo,material);
        if(existing.isEmpty())db.update("INSERT INTO t_lot(material_id,lot_no,production_date) VALUES(?,?,CURDATE())",material,lotNo);
        return id(db.queryForMap("SELECT id FROM t_lot WHERE lot_no=? AND material_id=?",lotNo,material).get("id"));
    }
    @Transactional
    public void move(long wo,long material,long lot,BigDecimal quantity,String kind,long source){move(wo,material,lot,quantity,kind,source,null);}
    @Transactional
    public void move(long wo,long material,long lot,BigDecimal quantity,String kind,long source,Long actor){
        require(quantity.signum()!=0,"线边变动数量不能为0");
        require(quantity.stripTrailingZeros().scale()<=2,"本次耗料精度超过库存两位小数，请按可精确核算的批量报工");
        require(db.queryForObject("SELECT COUNT(*) FROM t_lot l JOIN t_material m ON m.id=l.material_id WHERE l.id=? AND l.material_id=? AND l.is_deleted=0 AND m.status=1 AND (l.expire_date IS NULL OR l.expire_date>NOW())",Integer.class,lot,material)==1,"线边物料或批次已停用、过期，不能继续生产或转移");
        var warehouse=db.queryForList("SELECT id FROM t_warehouse WHERE warehouse_code='WH-A100-LINE' AND status=1");require(!warehouse.isEmpty(),"统一线边库未配置或已停用");long warehouseId=id(warehouse.get(0).get("id"));
        var locations=db.queryForList("SELECT id FROM t_location WHERE warehouse_id=? AND is_deleted=0 ORDER BY id LIMIT 1",warehouseId);require(!locations.isEmpty(),"统一线边库缺少有效库位");long location=id(locations.get(0).get("id"));
        if(actor==null){String actorSql=switch(kind){case "ISSUE"->"SELECT created_by FROM t_stock_out WHERE id=?";case "RECEIPT"->"SELECT created_by FROM t_stock_in WHERE id=?";case "CONSUME","OUTPUT"->"SELECT report_user_id FROM t_production_report WHERE id=?";case "SCRAP"->"SELECT approved_by FROM mm_nc WHERE id=?";default->"SELECT actor_id FROM mm_document WHERE id=?";};actor=db.queryForObject(actorSql,Long.class,source);}
        if(quantity.signum()>0)db.update("INSERT INTO mm_line_stock(work_order_id,material_id,lot_id,quantity) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE quantity=quantity+VALUES(quantity)",wo,material,lot,quantity);
        else require(db.update("UPDATE mm_line_stock SET quantity=quantity+? WHERE work_order_id=? AND material_id=? AND lot_id=? AND quantity>=?",quantity,wo,material,lot,quantity.negate())==1,"工单线边库存不足，不能消耗、退料或转库");
        db.update("INSERT INTO mm_line_movement(work_order_id,material_id,lot_id,quantity,kind,source_id) VALUES(?,?,?,?,?,?)",wo,material,lot,quantity,kind,source);
        String no="LINE-"+kind+"-"+wo+"-"+source;
        if(quantity.signum()>0)inventory.increaseStock(material,warehouseId,location,lot,quantity,"WIP_"+kind,source,no,actor,"工单 "+wo+" 线边过账");
        else inventory.decreaseStock(material,warehouseId,location,lot,quantity.negate(),"WIP_"+kind,source,no,actor,"工单 "+wo+" 线边过账");
    }
    public void issued(long wo,long document,List<com.mouse.mes.entity.StockOutItem> items){
        Map<List<Long>,BigDecimal> sums=new LinkedHashMap<>();for(var item:items){require(item.getLotId()!=null,"领料必须保留物料批次");sums.merge(List.of(item.getMaterialId(),item.getLotId()),item.getQuantity(),BigDecimal::add);}
        sums.forEach((k,q)->move(wo,k.get(0),k.get(1),q,"ISSUE",document));
    }
    @Transactional
    public void reported(long task,long report,int quantity){
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        var contexts=db.queryForList("SELECT * FROM mm_task_context WHERE task_id=?",task);if(contexts.isEmpty())return;var c=contexts.get(0);long wo=id(c.get("work_order_id"));
        var outputs=db.queryForList("SELECT * FROM mm_work_output WHERE work_order_id=? AND operation_id=?",wo,c.get("operation_id"));
        require(!outputs.isEmpty(),"历史工单尚未建立工序耗料快照，请先核对迁移，禁止自动推算历史消耗");
        for(var use:db.queryForList("SELECT * FROM mm_work_consumption WHERE work_order_id=? AND operation_id=? ORDER BY material_id",wo,c.get("operation_id"))){
            BigDecimal remaining=dec(use.get("quantity_per_unit")).multiply(BigDecimal.valueOf(quantity));
            var stocks=db.queryForList("SELECT * FROM mm_line_stock WHERE work_order_id=? AND material_id=? AND quantity>0 ORDER BY lot_id FOR UPDATE",wo,use.get("material_id"));
            for(var stock:stocks){if(remaining.signum()==0)break;BigDecimal take=dec(stock.get("quantity")).min(remaining);move(wo,id(use.get("material_id")),id(stock.get("lot_id")),take.negate(),"CONSUME",report);remaining=remaining.subtract(take);}
            require(remaining.signum()==0,"工序所需线边物料不足，请补领料或完成前序生产");
        }
        long output=id(outputs.get(0).get("material_id"));if(output>0)move(wo,output,outputLot(wo,output),BigDecimal.valueOf(quantity),"OUTPUT",report);
    }
    public void received(long wo,long document,long material,BigDecimal quantity){move(wo,material,outputLot(wo,material),quantity.negate(),"RECEIPT",document);}
    public List<Map<String,Object>> stock(){return db.queryForList("SELECT s.*,w.code work_order_code,m.material_code,m.material_name,m.unit,l.lot_no FROM mm_line_stock s JOIN mm_work_order w ON w.id=s.work_order_id JOIN t_material m ON m.id=s.material_id JOIN t_lot l ON l.id=s.lot_id WHERE s.quantity>0 ORDER BY s.work_order_id,s.material_id,s.lot_id");}
    public List<Map<String,Object>> movements(long wo){return db.queryForList("SELECT x.*,m.material_code,m.material_name,l.lot_no FROM mm_line_movement x JOIN t_material m ON m.id=x.material_id JOIN t_lot l ON l.id=x.lot_id WHERE x.work_order_id=? ORDER BY x.id",wo);}
}
