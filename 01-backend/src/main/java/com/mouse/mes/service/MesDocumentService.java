package com.mouse.mes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.entity.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Explicit draft/confirm/post documents; quantities are checked again under the global posting lock. */
@Service
public class MesDocumentService {
    private final JdbcTemplate db;private final MesRulesService rules;private final StockInService in;private final StockOutService out;private final LineStockService line;private final ObjectMapper json;
    private static final Set<String> KINDS=Set.of("PURCHASE_RECEIPT","PURCHASE_RETURN","PRODUCTION_RECEIPT","SALES_SHIPMENT","SALES_RETURN","LINE_RETURN","LINE_ISSUE");
    public MesDocumentService(JdbcTemplate db,MesRulesService rules,StockInService in,StockOutService out,LineStockService line,ObjectMapper json){this.db=db;this.rules=rules;this.in=in;this.out=out;this.line=line;this.json=json;}
    static long id(Object v){return MesRulesService.id(v);}static BigDecimal dec(Object v){return MesRulesService.dec(v);}static void require(boolean b,String m){MesRulesService.require(b,m);}
    private long count(String sql,Object...args){return Objects.requireNonNull(db.queryForObject(sql,Long.class,args));}
    private void lock(){db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");}
    public List<Map<String,Object>> documents(){return db.queryForList("SELECT d.*,w.warehouse_name,u.real_name actor_name FROM mm_document d JOIN t_warehouse w ON w.id=d.warehouse_id LEFT JOIN t_user u ON u.id=d.actor_id ORDER BY d.id DESC");}
    public List<Map<String,Object>> lines(long doc){return db.queryForList("SELECT l.*,m.material_code,m.material_name,b.lot_no,(SELECT status FROM mm_document_check c WHERE c.line_id=l.id ORDER BY id DESC LIMIT 1) qc_status FROM mm_document_line l JOIN t_material m ON m.id=l.material_id JOIN t_lot b ON b.id=l.lot_id WHERE document_id=? ORDER BY l.id",doc);}
    public List<Map<String,Object>> returnLots(String kind,long source){
        require(Set.of("PURCHASE_RETURN","SALES_RETURN").contains(kind),"只支持采购或销售退货批次");boolean sales=kind.equals("SALES_RETURN");String items=sales?"t_stock_out_item":"t_stock_in_item",headers=sales?"t_stock_out":"t_stock_in",fk=sales?"stock_out_id":"stock_in_id",type=sales?"stock_out_type":"stock_in_type";
        return db.queryForList("SELECT i.material_id,i.lot_id,l.lot_no,m.material_name,SUM(i.quantity) received_quantity,SUM(i.quantity)-COALESCE((SELECT SUM(dl.quantity) FROM mm_document_line dl JOIN mm_document d ON d.id=dl.document_id WHERE d.kind=? AND d.source_id=? AND d.status='POSTED' AND dl.material_id=i.material_id AND dl.lot_id=i.lot_id),0) quantity FROM "+items+" i JOIN "+headers+" h ON h.id=i."+fk+" JOIN t_lot l ON l.id=i.lot_id JOIN t_material m ON m.id=i.material_id WHERE h.status='COMPLETED' AND h.source_order_id=? AND h."+type+"=? GROUP BY i.material_id,i.lot_id,l.lot_no,m.material_name HAVING quantity>0",kind,source,source,sales?"SALES_SHIPMENT":"PURCHASE_RECEIPT");
    }
    @Transactional
    public long create(Map<String,Object> v,long actor){
        lock();String kind=String.valueOf(v.get("kind"));require(KINDS.contains(kind),"单据类型无效");long source=id(v.get("source_id")),warehouse=id(v.get("warehouse_id"));require(source>0,"必须选择真实业务来源");require(count("SELECT COUNT(*) FROM t_warehouse WHERE id=? AND status=1",warehouse)==1,"仓库不存在或未启用");
        require(count("SELECT COUNT(*) FROM t_warehouse WHERE id=? AND warehouse_code='WH-A100-LINE'",warehouse)==0,"请选择原仓库或目的仓库；统一线边库由工单台账自动处理");
        String reason=v.get("reason")==null?"":v.get("reason").toString();require(reason.length()<=500,"原因最多500字");if(kind.endsWith("RETURN"))require(!reason.isBlank(),"退货/退料必须填写原因");
        String rule=switch(kind){case "PURCHASE_RECEIPT"->"ITEMRECPT_CODE";case "PURCHASE_RETURN"->"WM_RTVENDOR_CODE";case "PRODUCTION_RECEIPT"->"PRODUCTRECPT_CODE";case "SALES_SHIPMENT"->"PRODUCTSALSE_CODE";case "SALES_RETURN"->"RTSALSE_CODE";case "LINE_ISSUE"->"ISSUE_CODE";default->"RTISSUE_CODE";};
        String code=rules.nextCode(rule);db.update("INSERT INTO mm_document(code,kind,source_id,warehouse_id,reason,actor_id) VALUES(?,?,?,?,?,?)",code,kind,source,warehouse,reason,actor);long doc=id(db.queryForMap("SELECT id FROM mm_document WHERE code=?",code).get("id"));
        String name=Objects.toString(v.get("name"),kind+" "+code);require(!name.isBlank()&&name.length()<=100,"单据名称须为1至100字");java.time.LocalDate date=v.get("business_date")==null?java.time.LocalDate.now():java.time.LocalDate.parse(v.get("business_date").toString());require(!date.isAfter(java.time.LocalDate.now()),"业务日期不能晚于今天");db.update("UPDATE mm_document SET name=?,business_date=? WHERE id=?",name,date,doc);

        Object raw=v.get("lines");require(raw instanceof List&&!((List<?>)raw).isEmpty()&&((List<?>)raw).size()<=200,"请添加1至200行物料");
        for(Object item:(List<?>)raw){require(item instanceof Map,"明细格式无效");Map<?,?> r=(Map<?,?>)item;long material=id(r.get("material_id"));BigDecimal entered=dec(r.get("quantity"));require(entered.signum()>0&&entered.stripTrailingZeros().scale()<=6,"录入数量须为正数且最多六位小数");long unit=id(r.get("unit_id"));BigDecimal q=rules.convert(material,unit,entered);require(q.signum()>0&&q.stripTrailingZeros().scale()<=2,"库存数量须为正数且最多两位小数");long lot=id(r.get("lot_id"));
            if(lot==0){require(kind.equals("PURCHASE_RECEIPT"),"请选择真实库存批次");String lotNo=String.valueOf(r.get("lot_no"));require(!lotNo.isBlank()&&!lotNo.equals("null")&&lotNo.length()<=64,"采购入库必须填写批次号，最多64字");var found=db.queryForList("SELECT id FROM t_lot WHERE lot_no=? AND material_id=?",lotNo,material);if(found.isEmpty())db.update("INSERT INTO t_lot(material_id,lot_no,production_date) VALUES(?,?,CURDATE())",material,lotNo);lot=id(db.queryForMap("SELECT id FROM t_lot WHERE lot_no=? AND material_id=?",lotNo,material).get("id"));}
            require(count("SELECT COUNT(*) FROM t_lot WHERE id=? AND material_id=?",lot,material)==1,"物料与批次不匹配");Long location=id(r.get("location_id"))==0?null:id(r.get("location_id"));if(location!=null)require(count("SELECT COUNT(*) FROM t_location WHERE id=? AND warehouse_id=? AND is_deleted=0",location,warehouse)==1,"库位不属于该仓库");
            require(count("SELECT COUNT(*) FROM mm_document_line WHERE document_id=? AND material_id=? AND lot_id=? AND COALESCE(location_id,0)=?",doc,material,lot,location==null?0:location)==0,"同一物料批次库位请合并数量");
            db.update("INSERT INTO mm_document_line(document_id,material_id,lot_id,location_id,quantity,source_line_id,input_unit_id,entered_quantity) VALUES(?,?,?,?,?,?,?,?)",doc,material,lot,location,q,id(r.get("source_line_id"))==0?null:id(r.get("source_line_id")),unit==0?null:unit,entered);
        }
        validateSource(db.queryForMap("SELECT * FROM mm_document WHERE id=?",doc));return doc;
    }
    @Transactional
    public void edit(long doc,Map<String,Object> body,long actor){
        lock();var existing=db.queryForMap("SELECT * FROM mm_document WHERE id=? FOR UPDATE",doc);require("DRAFT".equals(existing.get("status")),"只有草稿可以编辑，已确认或过账单据须保留原始凭据");
        var input=new LinkedHashMap<>(body);input.put("kind",existing.get("kind"));
        // Reuse every draft validation in this transaction, retaining the original public document number.
        long staged=create(input,actor);var replacement=db.queryForMap("SELECT * FROM mm_document WHERE id=?",staged);
        db.update("DELETE FROM mm_document_line WHERE document_id=?",doc);
        db.update("UPDATE mm_document_line SET document_id=? WHERE document_id=?",doc,staged);
        db.update("UPDATE mm_document SET name=?,business_date=?,source_id=?,warehouse_id=?,reason=? WHERE id=?",replacement.get("name"),replacement.get("business_date"),replacement.get("source_id"),replacement.get("warehouse_id"),replacement.get("reason"),doc);
        db.update("DELETE FROM mm_document WHERE id=?",staged);
        db.update("INSERT INTO mm_audit(actor_id,resource,action,record_id) VALUES(?,'mes:document','EDIT',?)",actor,doc);
    }
    private BigDecimal sumPosted(String kind,long source,long material,Long lot){return db.queryForObject("SELECT COALESCE(SUM(l.quantity),0) FROM mm_document_line l JOIN mm_document d ON d.id=l.document_id WHERE d.kind=? AND d.source_id=? AND d.status='POSTED' AND l.material_id=?"+(lot==null?"":" AND l.lot_id=?"),BigDecimal.class,lot==null?new Object[]{kind,source,material}:new Object[]{kind,source,material,lot});}
    private void validateSource(Map<String,Object> doc){
        String kind=doc.get("kind").toString();long source=id(doc.get("source_id")),docId=id(doc.get("id"));
        var grouped=db.queryForList("SELECT material_id,lot_id,SUM(quantity) quantity FROM mm_document_line WHERE document_id=? GROUP BY material_id,lot_id",docId);
        require(!grouped.isEmpty(),"单据没有明细");
        if(kind.startsWith("PURCHASE"))require(count("SELECT COUNT(*) FROM t_purchase_order WHERE id=? AND is_deleted=0 AND status IN ('APPROVED','PARTIAL_RECEIVED','COMPLETED')",source)==1,"采购来源必须为已审批采购订单");
        if(kind.equals("SALES_SHIPMENT"))require(count("SELECT COUNT(*) FROM t_order WHERE id=? AND deleted=0 AND order_status=2",source)==1,"销售来源必须为生产中订单");
        for(var r:grouped){long material=id(r.get("material_id")),lot=id(r.get("lot_id"));BigDecimal q=dec(r.get("quantity"));
            if(kind.equals("PURCHASE_RECEIPT")){
                BigDecimal ordered=db.queryForObject("SELECT COALESCE(SUM(quantity-received_quantity),0) FROM t_purchase_order_item WHERE po_id=? AND material_id=?",BigDecimal.class,source,material);
                BigDecimal allLines=db.queryForObject("SELECT SUM(quantity) FROM mm_document_line WHERE document_id=? AND material_id=?",BigDecimal.class,docId,material);require(allLines.compareTo(ordered)<=0,"到货数量超过采购剩余量或物料不属于该采购单");
            }else if(kind.equals("PURCHASE_RETURN")){
                BigDecimal original=db.queryForObject("SELECT COALESCE(SUM(i.quantity),0) FROM t_stock_in_item i JOIN t_stock_in s ON s.id=i.stock_in_id WHERE s.source_order_id=? AND s.stock_in_type='PURCHASE_RECEIPT' AND s.status='COMPLETED' AND i.material_id=? AND i.lot_id=?",BigDecimal.class,source,material,lot);
                require(sumPosted(kind,source,material,lot).add(q).compareTo(original)<=0,"退货批次未从该供应商采购入库，或累计退货超过收货数量");
            }else if(kind.equals("SALES_RETURN")){
                BigDecimal shipped=db.queryForObject("SELECT COALESCE(SUM(i.quantity),0) FROM t_stock_out_item i JOIN t_stock_out s ON s.id=i.stock_out_id WHERE s.source_order_id=? AND s.stock_out_type='SALES_SHIPMENT' AND i.material_id=? AND i.lot_id=?",BigDecimal.class,source,material,lot);
                require(sumPosted(kind,source,material,lot).add(q).compareTo(shipped)<=0,"退货批次不属于原订单，或累计退货超过发货数量");
            }else if(kind.equals("SALES_SHIPMENT")){
                var order=db.queryForMap("SELECT * FROM t_order WHERE id=?",source);
                require(count("SELECT COUNT(*) FROM mm_work_product wp JOIN mm_work_order w ON w.id=wp.work_order_id JOIN t_production_plan p ON p.id=w.plan_id JOIN t_lot l ON l.material_id=wp.material_id AND l.lot_no=CONCAT('FG-MO-',w.id) WHERE p.order_id=? AND wp.material_id=? AND l.id=? AND w.status='PRODUCTION_COMPLETED'",source,material,lot)>0,"发货批次必须来自此订单已放行完工的制造工单");
                BigDecimal legacy=db.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM mm_stock_posting WHERE kind='SHIP' AND source_id=?",BigDecimal.class,source);
                BigDecimal allPosted=db.queryForObject("SELECT COALESCE(SUM(l.quantity),0) FROM mm_document_line l JOIN mm_document d ON d.id=l.document_id WHERE d.kind='SALES_SHIPMENT' AND d.source_id=? AND d.status='POSTED'",BigDecimal.class,source);
                BigDecimal total=db.queryForObject("SELECT SUM(quantity) FROM mm_document_line WHERE document_id=?",BigDecimal.class,docId);require(total.add(allPosted).add(legacy).compareTo(dec(order.get("quantity")))<=0,"累计发货超过订单数量");
            }else if(kind.equals("PRODUCTION_RECEIPT")){
                require(count("SELECT COUNT(*) FROM mm_work_order w JOIN mm_work_product p ON p.work_order_id=w.id WHERE w.id=? AND w.status='PRODUCTION_COMPLETED' AND p.material_id=?",source,material)==1,"仅可入库已全部检验放行工单的产成品");require(lot==line.outputLot(source,material),"生产入库批次与工单不匹配");
            }else if(kind.equals("LINE_ISSUE")){
                require(count("SELECT COUNT(*) FROM mm_work_order WHERE id=? AND status IN('CREATED','IN_PROGRESS')",source)==1,"已冻结或已完成工单不能继续领料");
                BigDecimal required=db.queryForObject("SELECT COALESCE(SUM(required_quantity),0) FROM mm_work_material WHERE work_order_id=? AND material_id=?",BigDecimal.class,source,material);
                BigDecimal issued=db.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM mm_line_movement WHERE work_order_id=? AND material_id=? AND kind IN ('ISSUE','LINE_ISSUE','LINE_RETURN')",BigDecimal.class,source,material);
                BigDecimal total=db.queryForObject("SELECT SUM(quantity) FROM mm_document_line WHERE document_id=? AND material_id=?",BigDecimal.class,docId,material);require(issued.add(total).compareTo(required)<=0,"累计净领料超过工单BOM需求");
            }else if(kind.equals("LINE_RETURN"))require(count("SELECT COUNT(*) FROM mm_work_material WHERE work_order_id=? AND material_id=?",source,material)==1,"生产退料只可退回该工单原材料");
        }
    }
    @Transactional
    public void confirm(long doc){lock();var row=db.queryForMap("SELECT * FROM mm_document WHERE id=? FOR UPDATE",doc);require("DRAFT".equals(row.get("status")),"仅草稿可确认");validateSource(row);db.update("UPDATE mm_document SET status='CONFIRMED' WHERE id=?",doc);}
    @Transactional
    public void cancel(long doc){lock();require(db.update("UPDATE mm_document SET status='CANCELLED' WHERE id=? AND status IN ('DRAFT','CONFIRMED')",doc)==1,"已过账单据不能取消，应开反向单据");}
    @Transactional
    public void inspectLine(long lineId,Map<String,Object> v,long actor)throws Exception{
        lock();var row=db.queryForMap("SELECT l.*,d.kind,d.status document_status FROM mm_document_line l JOIN mm_document d ON d.id=l.document_id WHERE l.id=? FOR UPDATE",lineId);require(Set.of("DRAFT","CONFIRMED").contains(row.get("document_status")),"已过账或取消单据不能修改检验");
        long template=id(v.get("template_id"));var t=db.queryForMap("SELECT * FROM mm_qc_template WHERE id=?",template);String expected=row.get("kind").equals("PURCHASE_RECEIPT")?"INCOMING":"SHIPPING";require(expected.equals(t.get("kind")),"检测模板场景不匹配");
        if(expected.equals("INCOMING"))require(count("SELECT COUNT(*) FROM mm_bom_line l JOIN mm_bom b ON b.id=l.bom_id WHERE b.product_id=? AND l.material_id=?",t.get("product_id"),row.get("material_id"))>0,"检测模板不适用于此物料");
        else require(count("SELECT COUNT(*) FROM mm_product_material WHERE product_id=? AND material_id=?",t.get("product_id"),row.get("material_id"))>0,"出货检测模板产品不匹配");
        BigDecimal qualified=dec(v.get("qualified_quantity"));require(qualified.signum()>=0&&qualified.compareTo(dec(row.get("quantity")))<=0,"合格数量无效");boolean passed=qualified.compareTo(dec(row.get("quantity")))==0;
        require(v.get("results") instanceof Map,"请填写检测项结果");Map<?,?> results=(Map<?,?>)v.get("results");var items=db.queryForList("SELECT i.* FROM mm_qc_item i JOIN mm_qc_line l ON l.item_id=i.id WHERE l.template_id=?",template);require(!items.isEmpty(),"模板没有检测项");List<Object> snapshot=new ArrayList<>();
        for(var item:items){Object raw=results.get(String.valueOf(item.get("id")));require(raw instanceof Map,"检测项未填写完整");Map<?,?> r=(Map<?,?>)raw;String actual=String.valueOf(r.getOrDefault("actual",null));require(!actual.isBlank()&&!actual.equals("null"),"检测值不能为空");String verdict=String.valueOf(r.get("result"));require(Set.of("PASS","FAIL").contains(verdict),"检测结论无效");if("NUMBER".equals(item.get("data_type"))){BigDecimal a=dec(actual);verdict=a.compareTo(dec(item.get("lower_limit")))>=0&&a.compareTo(dec(item.get("upper_limit")))<=0?"PASS":"FAIL";}passed&=verdict.equals("PASS");snapshot.add(Map.of("item",item,"actual",actual,"result",verdict));}
        db.update("INSERT INTO mm_document_check(line_id,template_id,quantity,qualified_quantity,results,status,actor_id) VALUES(?,?,?,?,?,?,?)",lineId,template,row.get("quantity"),qualified,json.writeValueAsString(snapshot),passed?"RELEASED":"BLOCKED",actor);
    }
    @Transactional
    public void post(long doc,long actor){
        lock();var d=db.queryForMap("SELECT * FROM mm_document WHERE id=? FOR UPDATE",doc);if("POSTED".equals(d.get("status")))return;require("CONFIRMED".equals(d.get("status")),"请先确认单据再执行过账");validateSource(d);String kind=d.get("kind").toString();long source=id(d.get("source_id")),warehouse=id(d.get("warehouse_id"));var rows=lines(doc);
        boolean incoming=Set.of("PURCHASE_RECEIPT","PRODUCTION_RECEIPT","SALES_RETURN","LINE_RETURN").contains(kind);
        if(Set.of("PURCHASE_RECEIPT","SALES_SHIPMENT","SALES_RETURN").contains(kind))for(var r:rows)require("RELEASED".equals(r.get("qc_status")),"必须逐物料批次检验放行后才能过账");
        List<StockInItem> ins=new ArrayList<>();List<StockOutItem> outs=new ArrayList<>();
        for(var r:rows){long material=id(r.get("material_id")),lot=id(r.get("lot_id"));BigDecimal q=dec(r.get("quantity"));Long location=r.get("location_id")==null?null:id(r.get("location_id"));
            if(incoming){require(location!=null,"入库必须选择有效库位");StockInItem item=new StockInItem();item.setMaterialId(material);item.setLotId(lot);item.setLocationId(location);item.setQuantity(q);ins.add(item);}
            else if(location!=null){StockOutItem item=new StockOutItem();item.setMaterialId(material);item.setLotId(lot);item.setLocationId(location);item.setQuantity(q);outs.add(item);}
            else {BigDecimal left=q;for(var stock:db.queryForList("SELECT * FROM t_inventory WHERE warehouse_id=? AND material_id=? AND lot_id=? AND is_deleted=0 AND quantity>0 ORDER BY id FOR UPDATE",warehouse,material,lot)){if(left.signum()==0)break;BigDecimal take=dec(stock.get("quantity")).min(left);StockOutItem item=new StockOutItem();item.setMaterialId(material);item.setLotId(lot);item.setLocationId(stock.get("location_id")==null?null:id(stock.get("location_id")));item.setQuantity(take);outs.add(item);left=left.subtract(take);}require(left.signum()==0,"所选仓库批次库存不足");}
        }
        if(Set.of("LINE_RETURN","PRODUCTION_RECEIPT","LINE_ISSUE").contains(kind))for(var r:db.queryForList("SELECT material_id,lot_id,SUM(quantity) quantity FROM mm_document_line WHERE document_id=? GROUP BY material_id,lot_id",doc))line.move(source,id(r.get("material_id")),id(r.get("lot_id")),kind.equals("LINE_ISSUE")?dec(r.get("quantity")):dec(r.get("quantity")).negate(),kind,doc,actor);
        if(incoming){StockIn s=new StockIn();s.setStockInNo(d.get("code").toString());s.setStockInType(kind);s.setSourceOrderId(source);s.setSourceOrderNo(d.get("code").toString());s.setWarehouseId(warehouse);s.setCreatedBy(actor);s.setItems(ins);PostingScope.execute(()->in.create(s));}
        else {StockOut s=new StockOut();s.setStockOutNo(d.get("code").toString());s.setStockOutType(kind);s.setSourceOrderId(source);s.setSourceOrderNo(d.get("code").toString());s.setWarehouseId(warehouse);s.setCreatedBy(actor);s.setItems(outs);PostingScope.execute(()->out.create(s));}
        db.update("UPDATE mm_document SET status='POSTED',posted_at=NOW() WHERE id=?",doc);
        if(kind.equals("PURCHASE_RECEIPT")){
            for(var r:db.queryForList("SELECT material_id,SUM(quantity) quantity FROM mm_document_line WHERE document_id=? GROUP BY material_id",doc)){BigDecimal left=dec(r.get("quantity"));for(var item:db.queryForList("SELECT * FROM t_purchase_order_item WHERE po_id=? AND material_id=? AND received_quantity<quantity ORDER BY id FOR UPDATE",source,r.get("material_id"))){BigDecimal take=dec(item.get("quantity")).subtract(dec(item.get("received_quantity"))).min(left);db.update("UPDATE t_purchase_order_item SET received_quantity=received_quantity+? WHERE id=?",take,item.get("id"));left=left.subtract(take);if(left.signum()==0)break;}}
            db.update("UPDATE t_purchase_order SET status=IF(EXISTS(SELECT 1 FROM t_purchase_order_item WHERE po_id=? AND received_quantity<quantity),'PARTIAL_RECEIVED','COMPLETED') WHERE id=?",source,source);
        }
        if(kind.equals("SALES_SHIPMENT")){
            BigDecimal total=db.queryForObject("SELECT COALESCE(SUM(l.quantity),0) FROM mm_document_line l JOIN mm_document d ON d.id=l.document_id WHERE d.kind='SALES_SHIPMENT' AND d.source_id=? AND d.status='POSTED'",BigDecimal.class,source);
            if(total.compareTo(dec(db.queryForMap("SELECT quantity FROM t_order WHERE id=?",source).get("quantity")))==0)db.update("UPDATE t_order SET order_status=3 WHERE id=?",source);
        }
        db.update("INSERT INTO mm_audit(actor_id,resource,action,record_id) VALUES(?,'stock-documents',?,?)",actor,kind,doc);
    }
}
