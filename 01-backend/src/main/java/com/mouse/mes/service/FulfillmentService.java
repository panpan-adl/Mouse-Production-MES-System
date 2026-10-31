package com.mouse.mes.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.entity.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Full-batch material issue, quality-gated receipts and shipment share the existing stock ledger. */
@Service
public class FulfillmentService {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final StockInService stockIn;
    private final StockOutService stockOut;
    @org.springframework.beans.factory.annotation.Autowired private LineStockService lineStock;
    @org.springframework.beans.factory.annotation.Autowired private MesRulesService codes;
    public FulfillmentService(JdbcTemplate db,ObjectMapper json,StockInService stockIn,StockOutService stockOut){this.db=db;this.json=json;this.stockIn=stockIn;this.stockOut=stockOut;}
    public static long n(Object v){return v==null?0:new BigDecimal(v.toString()).longValueExact();}
    private static BigDecimal d(Object v){return v==null?BigDecimal.ZERO:new BigDecimal(v.toString());}
    private void require(boolean value,String message){if(!value)throw new IllegalArgumentException(message);}
    private long count(String sql,Object...args){return Objects.requireNonNull(db.queryForObject(sql,Long.class,args));}
    public void lock(){db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");}
    private Map<String,Object> one(String table,long id){var rows=db.queryForList("SELECT * FROM "+table+" WHERE id=?",id);require(!rows.isEmpty(),"来源记录不存在");return rows.get(0);}
    private void warehouse(long id){require(count("SELECT COUNT(*) FROM t_warehouse WHERE id=? AND status=1 AND warehouse_code<>'WH-A100-LINE'",id)==1,"请选择有效的外部仓库；统一线边库由工单作业自动过账");}
    private void location(long warehouse,Long location){warehouse(warehouse);require(location!=null&&count("SELECT COUNT(*) FROM t_location WHERE id=? AND warehouse_id=? AND is_deleted=0 AND status='1'",location,warehouse)==1,"请选择属于该仓库的有效库位");}
    public void freeze(long wo,List<Map<String,Object>> leaves,BigDecimal amount,long productMaterial){
        for(var m:leaves){BigDecimal quantity=d(m.get("required_quantity")).multiply(amount);require(quantity.scale()<=2||quantity.stripTrailingZeros().scale()<=2,"BOM展开数量精度不能超过两位小数");
            db.update("INSERT INTO mm_work_material(work_order_id,material_id,material_code,material_name,unit,required_quantity) VALUES(?,?,?,?,?,?)",wo,m.get("id"),m.get("material_code"),m.get("material_name"),m.get("unit"),quantity);}
        db.update("INSERT INTO mm_work_product(work_order_id,material_id,quantity) VALUES(?,?,?)",wo,productMaterial,amount.intValueExact());
    }
    /** Old stage-one work orders are expanded from their captured composition, never from a changed current BOM. */
    private void ensureFrozen(long wo){
        if(count("SELECT COUNT(*) FROM mm_work_product WHERE work_order_id=?",wo)>0)return;
        var w=one("mm_work_order",wo);var bom=one("mm_bom",n(w.get("bom_id")));
        try{
            List<Map<String,Object>> lines=json.readValue(w.get("bom_snapshot").toString(),new TypeReference<List<Map<String,Object>>>(){});
            Map<Long,BigDecimal> leaves=new LinkedHashMap<>();expand(n(bom.get("root_material_id")),BigDecimal.ONE,lines,leaves,new HashSet<>());
            List<Map<String,Object>> materials=new ArrayList<>();for(var leaf:leaves.entrySet()){var m=one("t_material",leaf.getKey());m.put("required_quantity",leaf.getValue());materials.add(m);}
            freeze(wo,materials,d(w.get("quantity")),n(bom.get("root_material_id")));
        }catch(IllegalArgumentException e){throw e;}catch(Exception e){throw new IllegalArgumentException("旧工单快照无法展开，请核查历史数据");}
    }
    private void expand(long id,BigDecimal qty,List<Map<String,Object>> lines,Map<Long,BigDecimal> leaves,Set<Long> active){
        require(active.add(id),"历史BOM存在循环");var children=lines.stream().filter(x->n(x.get("parent_material_id"))==id).toList();
        if(children.isEmpty()){require(n(one("t_material",id).get("material_type"))==1,"历史BOM缺少原材料组成");leaves.merge(id,qty,BigDecimal::add);}
        else for(var l:children)expand(n(l.get("material_id")),qty.multiply(d(l.get("quantity"))),lines,leaves,active);
        active.remove(id);
    }
    @Transactional
    public List<Map<String,Object>> readiness(long wo,Long warehouse){
        lock();ensureFrozen(wo);if(warehouse!=null)warehouse(warehouse);
        var rows=db.queryForList("SELECT * FROM mm_work_material WHERE work_order_id=? ORDER BY material_id",wo);
        boolean issued=count("SELECT COUNT(*) FROM mm_stock_posting WHERE kind='ISSUE' AND source_id=?",wo)>0;
        boolean lineHistory=count("SELECT COUNT(*) FROM mm_line_movement WHERE work_order_id=?",wo)>0;
        for(var row:rows){BigDecimal available=db.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM t_inventory WHERE is_deleted=0 AND warehouse_id NOT IN(SELECT id FROM t_warehouse WHERE warehouse_code='WH-A100-LINE') AND material_id=?"+(warehouse==null?"":" AND warehouse_id=?"),BigDecimal.class,warehouse==null?new Object[]{row.get("material_id")}:new Object[]{row.get("material_id"),warehouse});
            BigDecimal net=db.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM mm_line_movement WHERE work_order_id=? AND material_id=? AND kind IN ('ISSUE','LINE_ISSUE','LINE_RETURN')",BigDecimal.class,wo,row.get("material_id"));
            BigDecimal unissued=issued&&!lineHistory?BigDecimal.ZERO:d(row.get("required_quantity")).subtract(net).max(BigDecimal.ZERO);
            row.put("available_quantity",available);row.put("unissued_quantity",unissued);row.put("shortage_quantity",unissued.subtract(available).max(BigDecimal.ZERO));row.put("issued",unissued.signum()==0);}
        return rows;
    }
    public List<Map<String,Object>> workOrders(){return db.queryForList("SELECT w.id,w.code,w.plan_id,w.quantity,w.status,w.created_at,p.product_name,p.completed_quantity,p.order_id,o.order_no,(SELECT document_no FROM mm_stock_posting WHERE kind='ISSUE' AND source_id=w.id) issue_no,(SELECT document_no FROM mm_stock_posting WHERE kind='RECEIPT' AND source_id=w.id) receipt_no,(SELECT document_no FROM mm_stock_posting WHERE kind='SHIP' AND source_id=p.order_id) shipment_no FROM mm_work_order w JOIN t_production_plan p ON p.id=w.plan_id LEFT JOIN t_order o ON o.id=p.order_id ORDER BY w.id DESC");}
    public List<Map<String,Object>> postings(){return db.queryForList("SELECT p.*,u.real_name AS actor_name,w.warehouse_name FROM mm_stock_posting p LEFT JOIN t_user u ON u.id=p.actor_id JOIN t_warehouse w ON w.id=p.warehouse_id ORDER BY p.id DESC");}
    public List<Map<String,Object>> stock(){return db.queryForList("SELECT i.*,m.material_code,m.material_name,m.material_type,m.unit,w.warehouse_name,l.location_name,b.lot_no FROM t_inventory i JOIN t_material m ON m.id=i.material_id JOIN t_warehouse w ON w.id=i.warehouse_id LEFT JOIN t_location l ON l.id=i.location_id LEFT JOIN t_lot b ON b.id=i.lot_id WHERE i.is_deleted=0 ORDER BY i.material_id,i.id");}
    public List<Map<String,Object>> locations(){return db.queryForList("SELECT id,warehouse_id,location_code,location_name FROM t_location WHERE is_deleted=0 AND status='1'");}
    public List<Map<String,Object>> purchases(){return db.queryForList("SELECT p.*,s.supplier_name,(SELECT document_no FROM mm_stock_posting WHERE kind='PURCHASE' AND source_id=p.id) receipt_no FROM t_purchase_order p LEFT JOIN t_supplier s ON s.id=p.supplier_id WHERE p.is_deleted=0 ORDER BY p.id DESC");}
    public List<Map<String,Object>> purchaseItems(long id){return db.queryForList("SELECT i.*,m.material_name,m.material_code,m.unit FROM t_purchase_order_item i JOIN t_material m ON m.id=i.material_id WHERE i.po_id=?",id);}
    public List<Map<String,Object>> inspectionSources(String kind){return inspectionSources(kind,"FINAL");}
    public List<Map<String,Object>> inspectionSources(String kind,String phase){require(Set.of("FIRST","LAST","PATROL","FINAL").contains(phase),"检验阶段无效");return switch(kind){
        case "INCOMING"->db.queryForList("SELECT p.id,p.po_no name,(SELECT COALESCE(SUM(quantity),0) FROM t_purchase_order_item WHERE po_id=p.id) quantity FROM t_purchase_order p WHERE p.status IN ('APPROVED','PARTIAL_RECEIVED') AND p.is_deleted=0");
        case "PROCESS"->db.queryForList("SELECT t.id,t.task_no name,t.quantity,t.product_id,c.operation_id FROM t_production_task t JOIN mm_task_context c ON c.task_id=t.id WHERE "+(Set.of("FIRST","PATROL").contains(phase)?"t.task_status IN(1,2,3)":"t.task_status=3"));
        case "SHIPPING"->db.queryForList("SELECT o.id,o.order_no name,o.quantity,p.product_id FROM t_order o JOIN t_production_plan p ON p.order_id=o.id JOIN mm_work_order w ON w.plan_id=p.id WHERE o.order_status=2 AND o.deleted=0 AND p.plan_status=2 AND EXISTS(SELECT 1 FROM mm_stock_posting s WHERE s.kind='RECEIPT' AND s.source_id=w.id)");
        default->throw new IllegalArgumentException("检验类型无效");};}
    private Map<String,Object> existing(String kind,long source,long warehouse){var found=db.queryForList("SELECT * FROM mm_stock_posting WHERE kind=? AND source_id=?",kind,source);if(found.isEmpty())return null;require(n(found.get(0).get("warehouse_id"))==warehouse,"此业务已在其他仓库过账");return found.get(0);}
    private Map<String,Object> record(String kind,long source,long doc,String no,long warehouse,Long location,BigDecimal quantity,long actor){db.update("INSERT INTO mm_stock_posting(kind,source_id,document_id,document_no,warehouse_id,location_id,quantity,actor_id) VALUES(?,?,?,?,?,?,?,?)",kind,source,doc,no,warehouse,location,quantity,actor);db.update("INSERT INTO mm_audit(actor_id,resource,action,record_id) VALUES(?,'fulfillment',?,?)",actor,kind,source);return existing(kind,source,warehouse);}
    private StockIn incoming(String type,long source,String sourceNo,long warehouse,long actor,List<StockInItem> items){StockIn s=new StockIn();s.setStockInNo(codes.nextCode(type.equals("PURCHASE_RECEIPT")?"ITEMRECPT_CODE":"PRODUCTRECPT_CODE"));s.setStockInType(type);s.setSourceOrderId(source);s.setSourceOrderNo(sourceNo);s.setWarehouseId(warehouse);s.setCreatedBy(actor);s.setStatus("COMPLETED");s.setItems(items);return PostingScope.execute(()->stockIn.create(s));}
    private StockOut outgoing(String type,long source,String sourceNo,long warehouse,long actor,List<StockOutItem> items){StockOut s=new StockOut();s.setStockOutNo(codes.nextCode(type.equals("SALES_SHIPMENT")?"PRODUCTSALSE_CODE":"ISSUE_CODE"));s.setStockOutType(type);s.setSourceOrderId(source);s.setSourceOrderNo(sourceNo);s.setWarehouseId(warehouse);s.setCreatedBy(actor);s.setStatus("COMPLETED");s.setItems(items);return PostingScope.execute(()->stockOut.create(s));}
    private long lot(long material,String no){db.update("INSERT INTO t_lot(material_id,lot_no,production_date) VALUES(?,?,NOW())",material,no);return n(db.queryForMap("SELECT id FROM t_lot WHERE lot_no=?",no).get("id"));}
    private StockInItem inItem(long material,long lot,Long location,BigDecimal quantity){var i=new StockInItem();i.setMaterialId(material);i.setLotId(lot);i.setLocationId(location);i.setQuantity(quantity);return i;}
    private List<StockOutItem> allocate(long material,long warehouse,Long onlyLot,BigDecimal needed){
        var slots=db.queryForList("SELECT * FROM t_inventory WHERE material_id=? AND warehouse_id=? AND is_deleted=0 AND quantity>0 AND (location_id IS NULL OR EXISTS(SELECT 1 FROM t_location p WHERE p.id=t_inventory.location_id AND p.is_deleted=0 AND p.status='1'))"+(onlyLot==null?"":" AND lot_id=?")+" ORDER BY id FOR UPDATE",onlyLot==null?new Object[]{material,warehouse}:new Object[]{material,warehouse,onlyLot});
        List<StockOutItem> result=new ArrayList<>();BigDecimal remaining=needed;
        for(var slot:slots){BigDecimal take=d(slot.get("quantity")).min(remaining);if(take.signum()==0)break;var i=new StockOutItem();i.setMaterialId(material);i.setLocationId(slot.get("location_id")==null?null:n(slot.get("location_id")));i.setLotId(slot.get("lot_id")==null?null:n(slot.get("lot_id")));i.setQuantity(take);result.add(i);remaining=remaining.subtract(take);}
        require(remaining.signum()==0,"库存不足：物料 "+one("t_material",material).get("material_name")+" 还缺 "+remaining);return result;
    }
    @Transactional
    public Map<String,Object> issue(long wo,long warehouse,long actor){
        lock();warehouse(warehouse);var done=existing("ISSUE",wo,warehouse);if(done!=null)return done;
        var w=one("mm_work_order",wo);require(Set.of("CREATED","IN_PROGRESS").contains(w.get("status").toString()),"已完成的历史工单不能补做领料");ensureFrozen(wo);
        require(count("SELECT COUNT(*) FROM mm_line_movement WHERE work_order_id=? AND kind IN('ISSUE','LINE_ISSUE')",wo)==0,"工单已有分批领料，请通过分批单据补领，不能重复整批领料");
        List<StockOutItem> items=new ArrayList<>();for(var row:db.queryForList("SELECT * FROM mm_work_material WHERE work_order_id=? ORDER BY material_id",wo))items.addAll(allocate(n(row.get("material_id")),warehouse,null,d(row.get("required_quantity"))));
        var document=outgoing("PRODUCTION_ISSUE",wo,w.get("code").toString(),warehouse,actor,items);
        lineStock.issued(wo,document.getId(),items);
        return record("ISSUE",wo,document.getId(),document.getStockOutNo(),warehouse,null,d(w.get("quantity")),actor);
    }
    @Transactional
    public Map<String,Object> receiveProduction(long wo,long warehouse,Long location,long actor){
        lock();location(warehouse,location);var done=existing("RECEIPT",wo,warehouse);if(done!=null)return done;
        var w=one("mm_work_order",wo);require("PRODUCTION_COMPLETED".equals(w.get("status")),"工单所有工序须先完成并放行");
        require(count("SELECT COUNT(*) FROM mm_stock_posting WHERE kind='ISSUE' AND source_id=?",wo)+count("SELECT COUNT(*) FROM mm_document WHERE kind='LINE_ISSUE' AND source_id=? AND status='POSTED'",wo)>0,"工单尚未完成领料");ensureFrozen(wo);
        var product=db.queryForMap("SELECT * FROM mm_work_product WHERE work_order_id=?",wo);long lot=lineStock.outputLot(wo,n(product.get("material_id")));
        var document=incoming("PRODUCTION_RECEIPT",wo,w.get("code").toString(),warehouse,actor,List.of(inItem(n(product.get("material_id")),lot,location,d(w.get("quantity")))));
        lineStock.received(wo,document.getId(),n(product.get("material_id")),d(w.get("quantity")));
        return record("RECEIPT",wo,document.getId(),document.getStockInNo(),warehouse,location,d(w.get("quantity")),actor);
    }
    @Transactional
    public Map<String,Object> receivePurchase(long po,long warehouse,Long location,long actor){
        lock();location(warehouse,location);var done=existing("PURCHASE",po,warehouse);if(done!=null)return done;
        var order=one("t_purchase_order",po);require(n(order.get("is_deleted"))==0&&"APPROVED".equals(order.get("status")),"仅已审批且未入库的采购单可整批收货");
        var inspection=latestInspection("PURCHASE",po);var lines=purchaseItems(po);require(!lines.isEmpty(),"采购单没有明细");
        BigDecimal total=lines.stream().map(x->d(x.get("quantity"))).reduce(BigDecimal.ZERO,BigDecimal::add);
        require(d(inspection.get("quantity")).compareTo(total)==0,"来料检验数量必须与采购明细整批数量一致");
        List<StockInItem> items=new ArrayList<>();for(var item:lines){require(d(item.get("received_quantity")).signum()==0,"此采购单存在历史到货记录，不能重复整批入库");require(d(item.get("quantity")).signum()>0,"采购数量必须为正数");long material=n(item.get("material_id"));require(n(one("t_material",material).get("material_type"))==1,"采购收货仅接收原材料");items.add(inItem(material,lot(material,"PO-"+po+"-"+item.get("id")),location,d(item.get("quantity"))));}
        var document=incoming("PURCHASE_RECEIPT",po,order.get("po_no").toString(),warehouse,actor,items);
        db.update("UPDATE t_purchase_order_item SET received_quantity=quantity WHERE po_id=?",po);db.update("UPDATE t_purchase_order SET status='COMPLETED' WHERE id=?",po);
        return record("PURCHASE",po,document.getId(),document.getStockInNo(),warehouse,location,total,actor);
    }
    private Map<String,Object> latestInspection(String type,long id){var results=db.queryForList("SELECT * FROM mm_inspection WHERE source_type=? AND source_id=? ORDER BY id DESC LIMIT 1",type,id);require(!results.isEmpty()&&"RELEASED".equals(results.get(0).get("status")),"来源尚未检验放行，或最新检验不合格");return results.get(0);}
    @Transactional
    public Map<String,Object> ship(long orderId,long warehouse,long actor){
        lock();warehouse(warehouse);var done=existing("SHIP",orderId,warehouse);if(done!=null)return done;
        var order=one("t_order",orderId);require(n(order.get("deleted"))==0&&n(order.get("order_status"))==2,"仅生产中的有效销售订单可发货");
        require(count("SELECT COUNT(*) FROM mm_document WHERE kind='SALES_SHIPMENT' AND source_id=? AND status='POSTED'",orderId)==0,"订单已有分批发货，请通过分批单据完成交付，不能改为重复整批发货");
        var inspection=latestInspection("ORDER",orderId);require(d(inspection.get("quantity")).compareTo(d(order.get("quantity")))==0,"出货检验数量不匹配");
        var works=db.queryForList("SELECT w.* FROM mm_work_order w JOIN t_production_plan p ON p.id=w.plan_id WHERE p.order_id=?",orderId);require(works.size()==1,"本阶段每张销售订单须关联一张制造工单");var w=works.get(0);long wo=n(w.get("id"));
        require(existingReceipt(wo),"成品尚未入库");var product=db.queryForMap("SELECT * FROM mm_work_product WHERE work_order_id=?",wo);require(d(product.get("quantity")).compareTo(d(order.get("quantity")))==0,"订单与工单数量不一致");
        long lot=n(db.queryForMap("SELECT id FROM t_lot WHERE lot_no=?","FG-MO-"+wo).get("id"));
        var document=outgoing("SALES_SHIPMENT",orderId,order.get("order_no").toString(),warehouse,actor,allocate(n(product.get("material_id")),warehouse,lot,d(order.get("quantity"))));
        db.update("UPDATE t_order SET order_status=3 WHERE id=?",orderId);
        return record("SHIP",orderId,document.getId(),document.getStockOutNo(),warehouse,null,d(order.get("quantity")),actor);
    }
    private boolean existingReceipt(long wo){return count("SELECT COUNT(*) FROM mm_stock_posting WHERE kind='RECEIPT' AND source_id=?",wo)==1;}
    public void requireIssued(long task){var rows=db.queryForList("SELECT work_order_id FROM mm_task_context WHERE task_id=?",task);if(!rows.isEmpty())require(count("SELECT COUNT(*) FROM mm_stock_posting WHERE kind='ISSUE' AND source_id=?",rows.get(0).get("work_order_id"))+count("SELECT COUNT(*) FROM mm_document WHERE kind='LINE_ISSUE' AND status='POSTED' AND source_id=?",rows.get(0).get("work_order_id"))>0,"请先完成工单领料，再开工和报工");}
    public List<Map<String,Object>> trace(long wo){
        Map<Long,Map<String,Object>> all=new TreeMap<>();Set<Long> visited=new HashSet<>();Deque<Long> queue=new ArrayDeque<>();queue.add(wo);
        while(!queue.isEmpty()){long current=queue.removeFirst();require(visited.add(current)&&visited.size()<=100,"工单关联存在循环或层级过多");for(var r:traceSingle(current))all.put(n(r.get("id")),r);for(var child:db.queryForList("SELECT child_work_order_id FROM mm_work_relation WHERE parent_work_order_id=?",current))queue.add(n(child.get("child_work_order_id")));}
        return new ArrayList<>(all.values());
    }
    private List<Map<String,Object>> traceSingle(long wo){
        var w=one("mm_work_order",wo);var p=one("t_production_plan",n(w.get("plan_id")));Map<Long,Map<String,Object>> rows=new TreeMap<>();
        for(var r:legacyTrace(wo))rows.put(n(r.get("id")),r);
        for(var r:db.queryForList("SELECT t.*,m.material_name,l.lot_no FROM t_inventory_transaction t JOIN t_material m ON m.id=t.material_id LEFT JOIN t_lot l ON l.id=t.lot_id WHERE EXISTS(SELECT 1 FROM mm_line_movement x WHERE x.work_order_id=? AND x.material_id=t.material_id AND x.lot_id=t.lot_id AND t.business_type=CONCAT('WIP_',x.kind) AND t.source_order_id=x.source_id AND t.source_order_no=CONCAT('LINE-',x.kind,'-',x.work_order_id,'-',x.source_id))",wo))rows.put(n(r.get("id")),r);
        for(var r:db.queryForList("SELECT t.*,m.material_name,l.lot_no FROM t_inventory_transaction t JOIN t_material m ON m.id=t.material_id LEFT JOIN t_lot l ON l.id=t.lot_id WHERE EXISTS(SELECT 1 FROM mm_document d WHERE d.status='POSTED' AND d.code=t.source_order_no AND d.kind=t.business_type AND ((d.source_id=? AND d.kind IN ('PRODUCTION_RECEIPT','LINE_RETURN','LINE_ISSUE')) OR(d.source_id=? AND d.kind IN ('SALES_SHIPMENT','SALES_RETURN')))) OR (t.business_type='PURCHASE_RECEIPT' AND t.lot_id IN(SELECT lot_id FROM mm_line_movement WHERE work_order_id=? AND kind IN ('ISSUE','LINE_ISSUE')))",wo,p.get("order_id"),wo))rows.put(n(r.get("id")),r);
        return new ArrayList<>(rows.values());
    }
    private List<Map<String,Object>> legacyTrace(long wo){var w=one("mm_work_order",wo);var p=one("t_production_plan",n(w.get("plan_id")));return db.queryForList("SELECT t.*,m.material_name,l.lot_no FROM t_inventory_transaction t JOIN t_material m ON m.id=t.material_id LEFT JOIN t_lot l ON l.id=t.lot_id WHERE EXISTS(SELECT 1 FROM mm_stock_posting s WHERE s.document_no=t.source_order_no AND s.document_id=t.source_order_id AND ((s.kind='ISSUE' AND t.business_type='PRODUCTION_ISSUE') OR(s.kind='RECEIPT' AND t.business_type='PRODUCTION_RECEIPT') OR(s.kind='SHIP' AND t.business_type='SALES_SHIPMENT')) AND ((s.kind IN ('ISSUE','RECEIPT') AND s.source_id=?) OR(s.kind='SHIP' AND s.source_id=?))) OR (t.business_type='PURCHASE_RECEIPT' AND t.lot_id IN(SELECT q.lot_id FROM t_stock_out_item q JOIN mm_stock_posting s ON s.kind='ISSUE' AND s.document_id=q.stock_out_id WHERE s.source_id=?)) ORDER BY t.id",wo,p.get("order_id"),wo);}
}
