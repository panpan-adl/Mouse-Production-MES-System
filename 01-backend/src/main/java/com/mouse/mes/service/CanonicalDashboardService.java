package com.mouse.mes.service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Dashboard statistics use canonical work orders, real release events and posted stock balances. */
@Service
public class CanonicalDashboardService {
    private final JdbcTemplate db;
    public CanonicalDashboardService(JdbcTemplate db){this.db=db;}
    private double value(Object v){return v==null?0:new BigDecimal(v.toString()).doubleValue();}
    private double scalar(String sql,Object...args){return value(db.queryForObject(sql,Object.class,args));}
    private double rate(double a,double b){return b==0?0:Math.round(a/b*1000)/10.0;}
    private Map<String,Object> kpi(String key,String label,double value,String unit,String tone){return Map.of("key",key,"label",label,"value",value,"unit",unit,"tone",tone,"precision",1);}
    private Map<String,Object> node(String key,String name,int count,int completed){double r=rate(completed,count);return Map.of("key",key,"name",name,"count",count,"completionRate",r,"status",count==0||r>=80?"normal":r>=50?"warning":"error");}
    public Map<String,Object> summary(){
        var orders=db.queryForMap("SELECT COALESCE(SUM(o.order_status=0),0) pending,COALESCE(SUM(o.order_status=1),0) confirmed,COALESCE(SUM(o.order_status=2),0) producing,COALESCE(SUM(o.order_status=3),0) completed FROM t_order o WHERE o.deleted=0 AND EXISTS(SELECT 1 FROM t_production_plan p JOIN mm_work_order w ON w.plan_id=p.id WHERE p.order_id=o.id)");
        var plans=db.queryForMap("SELECT COALESCE(SUM(p.plan_status=0),0) pending,COALESCE(SUM(p.plan_status=1),0) inProgress,COALESCE(SUM(p.plan_status=2),0) completed,COALESCE(SUM(p.plan_quantity),0) totalQuantity,COALESCE(SUM(p.completed_quantity),0) completedQuantity FROM t_production_plan p JOIN mm_work_order w ON w.plan_id=p.id");plans.put("completionRate",rate(value(plans.get("completedQuantity")),value(plans.get("totalQuantity"))));
        var works=db.queryForMap("SELECT COALESCE(SUM(status='CREATED'),0) planned,0 assigned,COALESCE(SUM(status='IN_PROGRESS'),0) inProgress,COALESCE(SUM(status='PRODUCTION_COMPLETED'),0) completed FROM mm_work_order");
        return Map.of("orders",orders,"plans",plans,"workOrders",works,"totalTasks",scalar("SELECT COUNT(*) FROM mm_task_context"));
    }
    public List<Map<String,Object>> recentWorkOrders(){return db.queryForList("SELECT w.id,w.code AS woNo,w.quantity AS plannedQuantity,w.status,p.product_id AS productId,p.completed_quantity AS actualQuantity FROM mm_work_order w JOIN t_production_plan p ON p.id=w.plan_id ORDER BY w.id DESC LIMIT 10");}
    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public Map<String,Object> production(String requestedDate,String shift,String line){
        LocalDate date=requestedDate==null||requestedDate.isBlank()?LocalDate.now():LocalDate.parse(requestedDate);
        if(!Set.of("all","day","night").contains(shift))throw new IllegalArgumentException("班次筛选无效");
        LocalDateTime from=date.atStartOfDay(),to=from.plusDays(1);
        if(shift.equals("day")){from=date.atTime(8,0);to=date.atTime(20,0);}else if(shift.equals("night")){from=date.atTime(20,0);to=from.plusHours(12);}
        String workshop="";List<Object> scopeArgs=new ArrayList<>(List.of(date.toString()));
        if(!line.equals("all")){if(!line.startsWith("workshop:"))throw new IllegalArgumentException("请选择真实车间");long id=Long.parseLong(line.substring(9));workshop=" AND EXISTS(SELECT 1 FROM mm_task_context c JOIN mm_station s ON s.id=c.station_id WHERE c.work_order_id=w.id AND s.workshop_id=?)";scopeArgs.add(id);}
        String scope="SELECT w.id FROM mm_work_order w JOIN t_production_plan p ON p.id=w.plan_id WHERE COALESCE(p.start_date,DATE(w.created_at))=?"+workshop;
        var works=db.queryForList("SELECT w.*,p.product_name,p.completed_quantity,p.end_date,p.order_id FROM mm_work_order w JOIN t_production_plan p ON p.id=w.plan_id WHERE w.id IN ("+scope+") ORDER BY w.id DESC",scopeArgs.toArray());
        String ids=works.isEmpty()?"0":String.join(",",works.stream().map(w->w.get("id").toString()).toList());
        String event=" FROM mm_inspection i JOIN mm_task_context c ON c.task_id=i.source_id WHERE i.source_type='TASK' AND c.work_order_id IN ("+ids+")";
        double plan=works.stream().mapToDouble(w->value(w.get("quantity"))).sum();
        double done=scalar("SELECT COALESCE(SUM(i.quantity),0)"+event+" AND c.is_terminal=1 AND i.status='RELEASED' AND i.created_at>=? AND i.created_at<?",from,to);
        var firsts=db.queryForList("SELECT i.*"+event+" AND i.id=(SELECT MIN(x.id) FROM mm_inspection x WHERE x.source_type='TASK' AND x.source_id=i.source_id) AND i.created_at>=? AND i.created_at<?",from,to);
        double inspected=firsts.stream().mapToDouble(i->value(i.get("quantity"))).sum(),passed=firsts.stream().mapToDouble(i->value(i.get("qualified_quantity"))).sum();
        var shortages=db.queryForList("SELECT r.material_id,MAX(r.material_name) materialName,MAX(r.unit) unit,SUM(r.required_quantity) requiredQuantity,COALESCE((SELECT SUM(s.quantity) FROM t_inventory s WHERE s.material_id=r.material_id AND s.is_deleted=0),0) availableQuantity FROM mm_work_material r WHERE r.work_order_id IN ("+ids+") AND NOT EXISTS(SELECT 1 FROM mm_stock_posting p WHERE p.kind='ISSUE' AND p.source_id=r.work_order_id) GROUP BY r.material_id");
        List<Map<String,Object>> materialShortages=new ArrayList<>();
        for(var s:shortages){double shortage=Math.max(0,value(s.get("requiredQuantity"))-value(s.get("availableQuantity")));if(shortage>0)materialShortages.add(Map.of("materialName",s.get("materialName"),"unit",s.get("unit"),"shortageQuantity",shortage,"incomingQuantity",scalar("SELECT COALESCE(SUM(i.quantity-i.received_quantity),0) FROM t_purchase_order_item i JOIN t_purchase_order p ON p.id=i.po_id WHERE i.material_id=? AND p.is_deleted=0 AND p.status IN ('APPROVED','PARTIAL_RECEIVED')",s.get("material_id"))));}
        List<Map<String,Object>> alerts=new ArrayList<>();for(var s:materialShortages)alerts.add(Map.of("id","SHORT-"+alerts.size(),"time",LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),"type","error","title",s.get("materialName")+"缺料","content","未领料工单合计还缺 "+s.get("shortageQuantity")+" "+s.get("unit")));
        var counts=new LinkedHashMap<String,Integer>();List<Map<String,Object>> table=new ArrayList<>();int issued=0,received=0,shipped=0,completed=0;
        for(var w:works){long id=FulfillmentService.n(w.get("id"));boolean isIssued=scalar("SELECT COUNT(*) FROM mm_stock_posting WHERE kind='ISSUE' AND source_id=?",id)>0,isReceived=scalar("SELECT COUNT(*) FROM mm_stock_posting WHERE kind='RECEIPT' AND source_id=?",id)>0,isShipped=scalar("SELECT COUNT(*) FROM mm_stock_posting WHERE kind='SHIP' AND source_id=?",w.get("order_id"))>0;
            if(isIssued)issued++;if(isReceived)received++;if(isShipped)shipped++;boolean complete="PRODUCTION_COMPLETED".equals(w.get("status"));if(complete)completed++;
            String status=isShipped?"已发货":isReceived?"成品已入库":complete?"生产完成待入库":!isIssued?"待领料":"生产中";
            String node=isShipped?"ship":isReceived||complete?"stock":!isIssued?"bom":"work";
            counts.merge(status,1,Integer::sum);table.add(Map.of("id",w.get("code"),"productModel",w.get("product_name"),"batchNo","FG-MO-"+id,"planQuantity",w.get("quantity"),"completedQuantity",w.get("completed_quantity"),"dueDate",w.get("end_date")==null?"":w.get("end_date").toString(),"status",status,"statusTone",isShipped||isReceived?"green":complete?"yellow":"blue","nodeKey",node));
            if(!complete&&w.get("end_date")!=null&&LocalDate.parse(w.get("end_date").toString()).isBefore(LocalDate.now()))alerts.add(Map.of("id","LATE-"+id,"time",LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),"type","warning","title","工单逾期","content",w.get("code")+" 超过计划完成日期"));
        }
        List<Map<String,Object>> hourly=new ArrayList<>();for(LocalDateTime h=from;h.isBefore(to);h=h.plusHours(1)){double output=scalar("SELECT COALESCE(SUM(i.quantity),0)"+event+" AND c.is_terminal=1 AND i.status='RELEASED' AND i.created_at>=? AND i.created_at<?",h,h.plusHours(1));hourly.add(Map.of("hour",h.format(DateTimeFormatter.ofPattern("HH")),"plan",0,"actual",output));}
        List<Map<String,Object>> trend=new ArrayList<>();for(int i=6;i>=0;i--){LocalDate day=date.minusDays(i);double good=scalar("SELECT COALESCE(SUM(i.qualified_quantity),0)"+event+" AND i.id=(SELECT MIN(x.id) FROM mm_inspection x WHERE x.source_type='TASK' AND x.source_id=i.source_id) AND DATE(i.created_at)=?",day.toString()),total=scalar("SELECT COALESCE(SUM(i.quantity),0)"+event+" AND i.id=(SELECT MIN(x.id) FROM mm_inspection x WHERE x.source_type='TASK' AND x.source_id=i.source_id) AND DATE(i.created_at)=?",day.toString());trend.add(Map.of("label",day.toString().substring(5),"passed",good,"failed",total-good));}
        Map<String,Object> result=new LinkedHashMap<>();result.put("filters",Map.of("date",date.toString(),"shift",shift,"line",line));result.put("updatedAt",LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        result.put("kpis",List.of(kpi("plan","当日排程计划量",plan,"只","blue"),kpi("done","时段末道放行量",done,"只","green"),kpi("rate","计划达成率",rate(done,plan),"%","yellow"),kpi("pass","工序首检合格率",rate(passed,inspected),"%","green"),kpi("active","未完工工单数",works.size()-completed,"单","blue"),kpi("alerts","缺料与异常数",alerts.size(),"条","red")));
        result.put("hourlyOutput",hourly);result.put("workOrderStatus",counts.entrySet().stream().map(e->Map.of("label",e.getKey(),"value",e.getValue(),"tone","blue")).toList());result.put("materialShortages",materialShortages);result.put("materialReadinessRate",shortages.isEmpty()?(works.isEmpty()?0:100):rate(shortages.size()-materialShortages.size(),shortages.size()));
        result.put("flowNodes",List.of(node("order","关联订单",works.size(),shipped),node("plan","生产计划",works.size(),completed),node("bom","领料扣库",works.size(),issued),node("work","制造执行",works.size(),completed),node("qc","末道放行",works.size(),completed),node("stock","成品入库",works.size(),received),node("ship","销售发货",works.size(),shipped)));
        result.put("workOrders",table);result.put("qualityTrend",trend);
        result.put("defectRanks",db.queryForList("SELECT j.item_name defectType,COUNT(*) quantity FROM mm_inspection i JOIN JSON_TABLE(i.results,'$[*]' COLUMNS(item_name VARCHAR(100) PATH '$.item.name',verdict VARCHAR(20) PATH '$.result')) j ON TRUE WHERE i.source_type='TASK' AND i.source_id IN(SELECT task_id FROM mm_task_context WHERE work_order_id IN ("+ids+")) AND j.verdict='FAIL' AND i.created_at>=? AND i.created_at<? GROUP BY j.item_name ORDER BY quantity DESC",from,to));result.put("alerts",alerts);
        result.put("inventory",Map.of("rawMaterial",scalar("SELECT COALESCE(SUM(i.quantity),0) FROM t_inventory i JOIN t_material m ON m.id=i.material_id WHERE i.is_deleted=0 AND m.material_type=1"),"wip",scalar("SELECT COALESCE(SUM(i.quantity),0) FROM t_inventory i JOIN t_material m ON m.id=i.material_id WHERE i.is_deleted=0 AND m.material_type=2"),"finishedGoods",scalar("SELECT COALESCE(SUM(i.quantity),0) FROM t_inventory i JOIN t_material m ON m.id=i.material_id WHERE i.is_deleted=0 AND m.material_type=3"),"lowStockCount",scalar("SELECT COUNT(*) FROM t_material m WHERE m.status=1 AND m.safety_stock>COALESCE((SELECT SUM(i.quantity) FROM t_inventory i WHERE i.material_id=m.id AND i.is_deleted=0),0)")));
        result.put("materialSafety",db.queryForList("SELECT m.id,m.material_name,m.unit,m.safety_stock,COALESCE(SUM(i.quantity),0) current_quantity FROM t_material m LEFT JOIN t_inventory i ON i.material_id=m.id AND i.is_deleted=0 WHERE m.status=1 GROUP BY m.id ORDER BY (m.safety_stock-COALESCE(SUM(i.quantity),0)) DESC,m.id LIMIT 20"));
        return result;
    }
}
