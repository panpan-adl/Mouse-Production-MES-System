package com.mouse.mes.service;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MesPackagingService {
    private final JdbcTemplate db;private final MesRulesService rules;
    public MesPackagingService(JdbcTemplate db,MesRulesService rules){this.db=db;this.rules=rules;}
    static long id(Object v){return MesRulesService.id(v);}static BigDecimal dec(Object v){return MesRulesService.dec(v);}static void require(boolean b,String m){MesRulesService.require(b,m);}
    private long count(String q,Object...a){return Objects.requireNonNull(db.queryForObject(q,Long.class,a));}
    private void lock(){db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");}
    public List<Map<String,Object>> packages(){return db.queryForList("SELECT p.*,c.customer_name,o.order_no FROM mm_package p LEFT JOIN t_customer c ON c.id=p.customer_id LEFT JOIN t_order o ON o.id=p.order_id ORDER BY p.id DESC");}
    @Transactional
    public long create(Map<String,Object> v,long actor){
        lock();for(String f:List.of("length_mm","width_mm","height_mm","net_weight","gross_weight"))require(dec(v.get(f)).signum()>0,"长宽高及净重毛重必须为正数");require(dec(v.get("gross_weight")).compareTo(dec(v.get("net_weight")))>=0,"毛重不得小于净重");
        Long customer=id(v.get("customer_id"))==0?null:id(v.get("customer_id")),order=id(v.get("order_id"))==0?null:id(v.get("order_id"));if(customer!=null)require(count("SELECT COUNT(*) FROM t_customer WHERE id=? AND is_deleted=0",customer)==1,"客户不存在");
        if(order!=null){var o=db.queryForMap("SELECT * FROM t_order WHERE id=? AND deleted=0",order);require(customer!=null&&(id(o.get("customer_id"))==customer||id(o.get("customer_id"))==0&&count("SELECT COUNT(*) FROM t_customer WHERE id=? AND customer_name=? AND is_deleted=0",customer,o.get("customer_name"))==1),"订单与客户不匹配");}
        String code=rules.nextCode("PACKAGE_CODE");db.update("INSERT INTO mm_package(code,customer_id,order_id,length_mm,width_mm,height_mm,net_weight,gross_weight,created_by) VALUES(?,?,?,?,?,?,?,?,?)",code,customer,order,v.get("length_mm"),v.get("width_mm"),v.get("height_mm"),v.get("net_weight"),v.get("gross_weight"),actor);return id(db.queryForMap("SELECT id FROM mm_package WHERE code=?",code).get("id"));
    }
    @Transactional
    public void item(long box,long inventory,BigDecimal quantity){
        lock();var p=db.queryForMap("SELECT * FROM mm_package WHERE id=? FOR UPDATE",box);require("DRAFT".equals(p.get("status"))&&p.get("parent_id")==null,"只可编辑未装入父箱的草稿包装箱");require(count("SELECT COUNT(*) FROM mm_package WHERE parent_id=?",box)==0,"外箱不能同时直接装物料");require(quantity.signum()>0&&quantity.stripTrailingZeros().scale()<=2,"装箱数量须为正数且最多两位小数");
        var stock=db.queryForMap("SELECT * FROM t_inventory WHERE id=? AND is_deleted=0 FOR UPDATE",inventory);require(quantity.compareTo(dec(stock.get("quantity")))<=0,"装箱数量超过当前库存");
        StockPositionValidator.check(db,id(stock.get("material_id")),id(stock.get("warehouse_id")),stock.get("location_id")==null?null:id(stock.get("location_id")),stock.get("lot_id")==null?null:id(stock.get("lot_id")));
        if(p.get("order_id")!=null)require(count("SELECT COUNT(*) FROM mm_work_product wp JOIN mm_work_order w ON w.id=wp.work_order_id JOIN t_production_plan pl ON pl.id=w.plan_id JOIN t_lot l ON l.material_id=wp.material_id AND l.lot_no=CONCAT('FG-MO-',w.id) WHERE pl.order_id=? AND wp.material_id=? AND l.id=?",p.get("order_id"),stock.get("material_id"),stock.get("lot_id"))>0,"所装物料批次不属于此包装箱的订单");
        require(count("SELECT COUNT(*) FROM t_material WHERE id=? AND material_type IN(2,3)",stock.get("material_id"))==1,"只可装入半成品或成品");
        db.update("INSERT INTO mm_package_item(package_id,inventory_id,quantity) VALUES(?,?,?) ON DUPLICATE KEY UPDATE quantity=VALUES(quantity)",box,inventory,quantity);
    }
    @Transactional
    public void child(long parent,long child){
        lock();var p=db.queryForMap("SELECT * FROM mm_package WHERE id=? FOR UPDATE",parent);var c=db.queryForMap("SELECT * FROM mm_package WHERE id=? FOR UPDATE",child);require("DRAFT".equals(p.get("status"))&&p.get("parent_id")==null,"仅可编辑顶层草稿外箱");require("CONFIRMED".equals(c.get("status"))&&c.get("parent_id")==null,"子箱必须已确认且未装入其他箱");require(count("SELECT COUNT(*) FROM mm_package_item WHERE package_id=?",parent)==0,"有直接装箱物料的箱不能添加子箱");require(Objects.equals(p.get("customer_id"),c.get("customer_id"))&&Objects.equals(p.get("order_id"),c.get("order_id")),"父子箱的客户和订单必须一致");
        Set<Long> seen=new HashSet<>();long cursor=parent;while(cursor>0){require(cursor!=child&&seen.add(cursor),"包装箱不能循环嵌套");cursor=id(db.queryForMap("SELECT parent_id FROM mm_package WHERE id=?",cursor).get("parent_id"));}
        require(!descendants(child).contains(parent),"包装箱不能包含自身或祖先箱");db.update("UPDATE mm_package SET parent_id=? WHERE id=?",parent,child);
    }
    private Set<Long> descendants(long root){Set<Long> all=new LinkedHashSet<>();Deque<Long> queue=new ArrayDeque<>();queue.add(root);while(!queue.isEmpty()){long at=queue.removeFirst();require(all.add(at),"包装关系存在循环");require(all.size()<=10000,"单箱包含的子箱过多");for(var r:db.queryForList("SELECT id FROM mm_package WHERE parent_id=?",at))queue.add(id(r.get("id")));}return all;}
    public List<Map<String,Object>> contents(long box){List<Map<String,Object>> rows=new ArrayList<>();for(long child:descendants(box))rows.addAll(db.queryForList("SELECT p.code box_code,b.quantity,i.id inventory_id,m.material_code,m.material_name,l.lot_no,w.warehouse_name FROM mm_package_item b JOIN mm_package p ON p.id=b.package_id JOIN t_inventory i ON i.id=b.inventory_id JOIN t_material m ON m.id=i.material_id JOIN t_warehouse w ON w.id=i.warehouse_id LEFT JOIN t_lot l ON l.id=i.lot_id WHERE b.package_id=?",child));return rows;}
    @Transactional
    public void confirm(long box){
        lock();var p=db.queryForMap("SELECT * FROM mm_package WHERE id=? FOR UPDATE",box);require("DRAFT".equals(p.get("status")),"包装箱已确认");var content=contents(box);require(!content.isEmpty(),"空箱不能确认");
        for(var r:content){long inventory=id(r.get("inventory_id"));BigDecimal total=db.queryForObject("SELECT COALESCE(SUM(i.quantity),0) FROM mm_package_item i JOIN mm_package p ON p.id=i.package_id WHERE i.inventory_id=? AND (p.status='CONFIRMED' OR p.id=?)",BigDecimal.class,inventory,box);BigDecimal stock=db.queryForObject("SELECT quantity FROM t_inventory WHERE id=? AND is_deleted=0 FOR UPDATE",BigDecimal.class,inventory);require(stock!=null&&total.compareTo(stock)<=0,"同一库存已被其他箱使用或库存不足，请核对装箱数量");}
        db.update("UPDATE mm_package SET status='CONFIRMED' WHERE id=?",box);
    }
    public Map<String,Object> barcode(String kind,long id){
        // SQL identifiers are a closed mapping, never supplied by the client.
        String[] mapping=switch(kind){case "MATERIAL"->new String[]{"t_material","material_code"};case "WAREHOUSE"->new String[]{"t_warehouse","warehouse_code"};case "CUSTOMER"->new String[]{"t_customer","customer_code"};case "SUPPLIER"->new String[]{"t_supplier","supplier_code"};case "EQUIPMENT"->new String[]{"t_equipment","equipment_code"};case "STATION"->new String[]{"mm_station","code"};case "PACKAGE"->new String[]{"mm_package","code"};case "INVENTORY"->new String[]{"t_inventory","id"};default->throw new IllegalArgumentException("赋码对象类型无效");};
        var r=db.queryForMap("SELECT "+mapping[1]+" code FROM "+mapping[0]+" WHERE id=?",id);return Map.of("type",kind,"id",id,"content",kind+"-"+r.get("code"));
    }
    public Map<String,Object> resolve(String content){
        require(content!=null&&content.length()<=180&&content.contains("-"),"不是有效的MES生产要素编码");int split=content.indexOf('-');String kind=content.substring(0,split),code=content.substring(split+1);require(!code.isBlank()&&!code.contains("\n"),"编码内容无效");
        String[] mapping=switch(kind){case "MATERIAL"->new String[]{"t_material","material_code"};case "WAREHOUSE"->new String[]{"t_warehouse","warehouse_code"};case "CUSTOMER"->new String[]{"t_customer","customer_code"};case "SUPPLIER"->new String[]{"t_supplier","supplier_code"};case "EQUIPMENT"->new String[]{"t_equipment","equipment_code"};case "STATION"->new String[]{"mm_station","code"};case "PACKAGE"->new String[]{"mm_package","code"};case "INVENTORY"->new String[]{"t_inventory","id"};default->throw new IllegalArgumentException("不支持的生产要素编码");};
        require(!kind.equals("INVENTORY")||code.matches("[1-9][0-9]*"),"库存编号必须为正整数");var row=db.queryForMap("SELECT id FROM "+mapping[0]+" WHERE "+mapping[1]+"=?",code);return barcode(kind,id(row.get("id")));
    }
}
