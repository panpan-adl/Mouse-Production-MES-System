package com.mouse.mes.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fixed, parameterized business tools shared by the form and natural-language entry. */
@Service
public class BusinessQueryService {
    private final JdbcTemplate db;private final FulfillmentService fulfillment;private final ObjectMapper json;
    public BusinessQueryService(JdbcTemplate db,FulfillmentService fulfillment,ObjectMapper json){this.db=db;this.fulfillment=fulfillment;this.json=json;}
    public static final Set<String> FIELDS=Set.of("kind","material","warehouse","type","from","to","document","product");
    public Map<String,Object> catalog(int role){allow(role,"INVENTORY");return Map.of("materials",db.queryForList("SELECT id,material_code,material_name,unit,material_type FROM t_material WHERE status=1"),"warehouses",db.queryForList("SELECT id,warehouse_code,warehouse_name FROM t_warehouse WHERE status=1"),"products",db.queryForList("SELECT id,product_name FROM t_product WHERE status=1"));}
    public void allow(int role,String kind){if(!Set.of("INVENTORY","SHORTAGE","TRACE").contains(kind))throw new IllegalArgumentException("仅支持库存、缺料和工单追溯三个查询工具");if(!Set.of(1,2,4,5,6,7,9,10).contains(role)||(!kind.equals("TRACE")&&!Set.of(1,2,5,6,9,10).contains(role)))throw new SecurityException("当前岗位无权查询此类数据");}
    private String text(Map<String,?> f,String k){return Objects.toString(f.get(k),"").trim();}
    private BigDecimal num(Object v){return v==null?BigDecimal.ZERO:new BigDecimal(v.toString());}
    private BigDecimal scalar(String sql,Object...a){return num(db.queryForObject(sql,Object.class,a));}
    @Transactional(readOnly=true)
    public Map<String,Object> execute(Map<String,String> filters,int role){
        if(!FIELDS.containsAll(filters.keySet()))throw new IllegalArgumentException("存在非白名单条件");
        String kind=filters.getOrDefault("kind","INVENTORY");allow(role,kind);
        Map<String,Object> result=new LinkedHashMap<>();result.put("conditions",filters);result.put("updatedAt",LocalDateTime.now().toString());result.put("kind",kind);
        if(kind.equals("TRACE")){trace(filters,result);return result;}
        List<Object> args=new ArrayList<>();String where=" WHERE m.status=1";
        if(!text(filters,"material").isEmpty()){where+=" AND m.id=?";args.add(Long.parseLong(filters.get("material")));}
        if(!text(filters,"type").isEmpty()){int type=Integer.parseInt(filters.get("type"));if(type<1||type>3)throw new IllegalArgumentException("物料类别无效");where+=" AND m.material_type=?";args.add(type);}
        if(!text(filters,"product").isEmpty()){where+=" AND (EXISTS(SELECT 1 FROM mm_bom b JOIN mm_bom_line l ON l.bom_id=b.id WHERE b.product_id=? AND l.material_id=m.id) OR EXISTS(SELECT 1 FROM mm_product_material pm WHERE pm.product_id=? AND pm.material_id=m.id))";long p=Long.parseLong(filters.get("product"));args.add(p);args.add(p);}
        var materials=db.queryForList("SELECT m.id,m.material_code,m.material_name,m.unit,m.safety_stock,m.material_type FROM t_material m"+where+" ORDER BY m.id LIMIT 201",args.toArray());
        if(materials.size()>200)throw new IllegalArgumentException("范围超过200种物料，请增加条件");
        Long warehouse=text(filters,"warehouse").isEmpty()?null:Long.parseLong(filters.get("warehouse"));
        var warehouses=warehouse==null?db.queryForList("SELECT id,warehouse_name FROM t_warehouse WHERE status=1"):db.queryForList("SELECT id,warehouse_name FROM t_warehouse WHERE status=1 AND id=?",warehouse);
        if(warehouse!=null&&warehouses.isEmpty())throw new IllegalArgumentException("仓库不存在");
        var anchor=db.queryForMap("SELECT * FROM mm_inventory_anchor WHERE id=1");
        LocalDateTime captured=(anchor.get("captured_at") instanceof LocalDateTime ldt?ldt:((java.sql.Timestamp)anchor.get("captured_at")).toLocalDateTime());long watermark=((Number)anchor.get("ledger_id")).longValue();
        LocalDateTime from=text(filters,"from").isEmpty()?null:LocalDate.parse(filters.get("from")).atStartOfDay(),to=text(filters,"to").isEmpty()?null:LocalDate.parse(filters.get("to")).plusDays(1).atStartOfDay();
        if((from==null)!=(to==null))throw new IllegalArgumentException("请同时填写起止日期");
        if(from!=null&&(!from.isBefore(to)||to.isAfter(LocalDate.now().plusDays(1).atStartOfDay())))throw new IllegalArgumentException("日期范围无效或包含未来日期");
        if(kind.equals("SHORTAGE")&&(from!=null))throw new IllegalArgumentException("缺料查询使用当前库存，请清除历史日期");
        if(kind.equals("SHORTAGE")&&warehouse!=null&&scalar("SELECT COUNT(*) FROM t_warehouse WHERE id=? AND warehouse_code='WH-A100-LINE'",warehouse).signum()>0)throw new IllegalArgumentException("线边库存已经分配给工单，不作为其他工单的可用库存，请选择外部仓库");
        Map<Long,Long> origins=new HashMap<>();for(var origin:db.queryForList("SELECT material_id,ledger_id FROM mm_material_origin"))origins.put(((Number)origin.get("material_id")).longValue(),((Number)origin.get("ledger_id")).longValue());
        boolean historyIncomplete=false;
        List<Map<String,Object>> rows=new ArrayList<>();
        for(var m:materials){long mid=((Number)m.get("id")).longValue();
            if(kind.equals("SHORTAGE")){
                BigDecimal current=warehouse==null?scalar("SELECT COALESCE(SUM(quantity),0) FROM t_inventory WHERE material_id=? AND is_deleted=0 AND warehouse_id NOT IN(SELECT id FROM t_warehouse WHERE warehouse_code='WH-A100-LINE')",mid):scalar("SELECT COALESCE(SUM(quantity),0) FROM t_inventory WHERE material_id=? AND warehouse_id=? AND is_deleted=0",mid,warehouse);
                BigDecimal required=scalar("SELECT COALESCE(SUM(GREATEST(0,r.required_quantity-COALESCE((SELECT SUM(x.quantity) FROM mm_line_movement x WHERE x.work_order_id=r.work_order_id AND x.material_id=r.material_id AND x.kind IN('ISSUE','LINE_ISSUE','LINE_RETURN')),0))),0) FROM mm_work_material r WHERE r.material_id=? AND (NOT EXISTS(SELECT 1 FROM mm_stock_posting s WHERE s.kind='ISSUE' AND s.source_id=r.work_order_id) OR EXISTS(SELECT 1 FROM mm_line_movement lm WHERE lm.work_order_id=r.work_order_id))",mid);
                BigDecimal target=required.max(num(m.get("safety_stock"))),shortage=target.subtract(current).max(BigDecimal.ZERO);
                if(shortage.signum()>0){var r=new LinkedHashMap<String,Object>(m);r.put("current_quantity",current);r.put("required_quantity",required);r.put("shortage_quantity",shortage);r.put("warehouse",warehouse==null?"全部仓库":"指定仓库可用量；需求为全部未领料工单");rows.add(r);}continue;
            }
            for(var w:warehouses){long wid=((Number)w.get("id")).longValue();var r=new LinkedHashMap<String,Object>(m);r.put("warehouse_id",wid);r.put("warehouse_name",w.get("warehouse_name"));
                BigDecimal current=scalar("SELECT COALESCE(SUM(quantity),0) FROM t_inventory WHERE material_id=? AND warehouse_id=? AND is_deleted=0",mid,wid);r.put("current_quantity",current);
                if(from!=null){
                    r.put("in_quantity",scalar("SELECT COALESCE(SUM(quantity),0) FROM t_inventory_transaction WHERE material_id=? AND warehouse_id=? AND transaction_type='IN' AND created_at>=? AND created_at<?",mid,wid,from,to));
                    r.put("out_quantity",scalar("SELECT COALESCE(SUM(quantity),0) FROM t_inventory_transaction WHERE material_id=? AND warehouse_id=? AND transaction_type='OUT' AND created_at>=? AND created_at<?",mid,wid,from,to));
                    boolean anchored=!from.isBefore(captured),born=origins.containsKey(mid);boolean known=anchored||born;historyIncomplete|=!known;
                    r.put("opening_quantity",anchored?balance(mid,wid,from,watermark):born?originBalance(mid,wid,from,origins.get(mid)):null);
                    r.put("closing_quantity",anchored?balance(mid,wid,to,watermark):born?originBalance(mid,wid,to,origins.get(mid)):null);
                    r.put("balance_basis",anchored?"库存基线及后续流水":born?"新建物料零库存起点及完整流水":"缺少可信历史起点");
                }rows.add(r);
            }
        }
        result.put("rows",rows);result.put("baselineAt",captured.toString());result.put("evidence",historyIncomplete?"部分物料缺少查询起点的可信历史；对应期初/期末无法核实，请查看逐行依据。":"当前余额来自库存台账；历史余额由可信基线或新建物料零库存起点及完整流水核算。");
        String ledgerWhere=" WHERE 1=1";List<Object> ledgerArgs=new ArrayList<>();if(from!=null){ledgerWhere+=" AND t.created_at>=? AND t.created_at<?";ledgerArgs.add(from);ledgerArgs.add(to);}if(warehouse!=null){ledgerWhere+=" AND t.warehouse_id=?";ledgerArgs.add(warehouse);}
        Set<Long> mids=new HashSet<>();for(var m:materials)mids.add(((Number)m.get("id")).longValue());
        // IDs below originate exclusively from the parameterized catalog query, never from model output.
        String ids=mids.isEmpty()?"0":String.join(",",mids.stream().map(String::valueOf).toList());ledgerWhere+=" AND t.material_id IN ("+ids+")";
        result.put("sources",db.queryForList("SELECT t.transaction_no,t.source_order_no,t.transaction_type,t.quantity,t.created_at,m.material_name,m.unit,w.warehouse_name,b.lot_no,p.location_name FROM t_inventory_transaction t LEFT JOIN t_lot b ON b.id=t.lot_id LEFT JOIN t_location p ON p.id=t.location_id JOIN t_material m ON m.id=t.material_id JOIN t_warehouse w ON w.id=t.warehouse_id"+ledgerWhere+" ORDER BY t.id DESC LIMIT 201",ledgerArgs.toArray()));result.put("sourceLimit",200);return result;
    }
    private BigDecimal originBalance(long material,long warehouse,LocalDateTime at,long watermark){return scalar("SELECT COALESCE(SUM(IF(transaction_type='IN',quantity,-quantity)),0) FROM t_inventory_transaction WHERE id>? AND material_id=? AND warehouse_id=? AND created_at<?",watermark,material,warehouse,at);}
    private BigDecimal balance(long material,long warehouse,LocalDateTime at,long watermark){return scalar("SELECT COALESCE(SUM(quantity),0) FROM mm_inventory_baseline WHERE material_id=? AND warehouse_id=?",material,warehouse).add(scalar("SELECT COALESCE(SUM(IF(transaction_type='IN',quantity,-quantity)),0) FROM t_inventory_transaction WHERE id>? AND material_id=? AND warehouse_id=? AND created_at<?",watermark,material,warehouse,at));}
    private void trace(Map<String,String> f,Map<String,Object> out){
        String code=text(f,"document");if(code.isEmpty())throw new IllegalArgumentException("请提供准确工单、计划或销售订单编号");
        var works=db.queryForList("SELECT w.id,w.code,w.quantity,w.status,p.plan_no,o.order_no FROM mm_work_order w JOIN t_production_plan p ON p.id=w.plan_id LEFT JOIN t_order o ON o.id=p.order_id WHERE w.code=? OR p.plan_no=? OR o.order_no=?",code,code,code);
        if(works.isEmpty())throw new IllegalArgumentException("未找到关联正式工单，请核对单号");
        List<Map<String,Object>> rows=new ArrayList<>(),sources=new ArrayList<>(),reports=new ArrayList<>(),inspections=new ArrayList<>();
        for(var w:works){long id=((Number)w.get("id")).longValue();rows.add(w);sources.addAll(fulfillment.trace(id));
            reports.addAll(db.queryForList("SELECT r.* FROM t_production_report r JOIN mm_task_context c ON c.task_id=r.task_id WHERE c.work_order_id=? ORDER BY r.id",id));
            inspections.addAll(db.queryForList("SELECT i.* FROM mm_inspection i JOIN mm_task_context c ON c.task_id=i.source_id WHERE i.source_type='TASK' AND c.work_order_id=? ORDER BY i.id",id));}
        out.put("reports",reports);out.put("inspections",inspections);out.put("rows",rows);out.put("sources",sources);out.put("evidence","工单关联的报工、过程检验和采购/领料/成品/发货库存流水；单位见物料主数据。");
    }
}
