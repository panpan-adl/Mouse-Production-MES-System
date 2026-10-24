package com.mouse.mes.service;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MesOperationsService {
    @org.springframework.beans.factory.annotation.Autowired private AssignableStaffService assignableStaff;
    @org.springframework.beans.factory.annotation.Autowired private LineStockService lineStock;
    private final JdbcTemplate db;private final MesRulesService rules;private final FoundationService foundation;
    public MesOperationsService(JdbcTemplate db,MesRulesService rules,FoundationService foundation){this.db=db;this.rules=rules;this.foundation=foundation;}
    static long id(Object v){return MesRulesService.id(v);}static BigDecimal dec(Object v){return MesRulesService.dec(v);}static void require(boolean b,String m){MesRulesService.require(b,m);}
    private long count(String sql,Object...args){return Objects.requireNonNull(db.queryForObject(sql,Long.class,args));}
    @Transactional
    public long split(long task,int quantity){
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");var t=db.queryForMap("SELECT * FROM t_production_task WHERE id=? FOR UPDATE",task);
        require(id(t.get("task_status"))<=1&&count("SELECT COUNT(*) FROM t_production_report WHERE task_id=?",task)==0,"只有尚未开工报工的任务可以拆分");require(quantity>0&&quantity<id(t.get("quantity")),"拆分数量必须小于原任务量且大于0");
        String code=t.get("task_no")+"-S"+UUID.randomUUID().toString().substring(0,8);
        db.update("INSERT INTO t_production_task(task_no,plan_id,task_type,product_id,product_name,quantity,assigned_user_id,assigned_user_name,task_status) VALUES(?,?,?,?,?,?,?,?,1)",code,t.get("plan_id"),t.get("task_type"),t.get("product_id"),t.get("product_name"),quantity,t.get("assigned_user_id"),t.get("assigned_user_name"));long child=id(db.queryForMap("SELECT id FROM t_production_task WHERE task_no=?",code).get("id"));
        db.update("INSERT INTO mm_task_context(task_id,work_order_id,operation_id,sequence_no,predecessor_id,requires_qc,is_terminal,station_id) SELECT ?,work_order_id,operation_id,sequence_no,predecessor_id,requires_qc,is_terminal,station_id FROM mm_task_context WHERE task_id=?",child,task);
        db.update("UPDATE t_production_task SET quantity=quantity-? WHERE id=?",quantity,task);db.update("DELETE FROM mm_task_slot WHERE task_id=?",task);return child;
    }
    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> createWork(Map<String,Object> v,long actor)throws Exception{
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");long product=id(v.get("product_id")),order=id(v.get("order_id"));int quantity=Math.toIntExact(id(v.get("quantity")));require(quantity>0,"工单数量必须为正整数");var p=db.queryForMap("SELECT * FROM t_product WHERE id=?",product);
        if(order>0){var o=db.queryForMap("SELECT * FROM t_order WHERE id=? AND deleted=0 FOR UPDATE",order);require(id(o.get("order_status"))<3&&o.get("product_name").equals(p.get("product_name")),"订单状态或产品不匹配");long planned=count("SELECT COALESCE(SUM(p.plan_quantity),0) FROM t_production_plan p WHERE p.order_id=? AND NOT EXISTS(SELECT 1 FROM mm_work_order w WHERE w.plan_id=p.id AND w.status='SCRAP_HOLD')",order);require(planned+quantity<=id(o.get("quantity")),"订单计划总量不能超过订单数量");}
        String planNo="PLAN-"+rules.nextCode("WORKORDER_CODE");db.update("INSERT INTO t_production_plan(plan_no,order_id,product_id,product_name,plan_quantity,completed_quantity,start_date,end_date,plan_status,create_by) VALUES(?,?,?,?,?,0,CURDATE(),DATE_ADD(CURDATE(),INTERVAL 7 DAY),0,?)",planNo,order>0?order:null,product,p.get("product_name"),quantity,actor);
        long plan=id(db.queryForMap("SELECT id FROM t_production_plan WHERE plan_no=?",planNo).get("id"));Map<String,Object> payload=new LinkedHashMap<>(v);payload.put("plan_id",plan);return foundation.createWorkOrder(payload,actor);
    }
    @Transactional
    public void dispatch(long task,Map<String,Object> v){
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");var t=db.queryForMap("SELECT * FROM t_production_task WHERE id=? FOR UPDATE",task);
        require(id(t.get("task_status"))<=2&&count("SELECT COUNT(*) FROM t_production_report WHERE task_id=?",task)==0,"任务已报工或完成，不能重新排产");
        long station=id(v.get("station_id")),owner=id(v.get("owner_id"));rules.equipmentAvailable(station);
        assignableStaff.require(owner,3);
        require(count("SELECT COUNT(*) FROM mm_station_resource WHERE station_id=? AND resource_type='USER'",station)==0||count("SELECT COUNT(*) FROM mm_station_resource WHERE station_id=? AND resource_type='USER' AND user_id=?",station,owner)>0,"负责人不属于此工作站的人员资源");
        LocalDateTime start=LocalDateTime.parse(v.get("start_at").toString().replace(' ','T')),end=LocalDateTime.parse(v.get("end_at").toString().replace(' ','T'));require(end.isAfter(start),"结束时间必须晚于开始时间");
        var covering=db.queryForList("SELECT s.id FROM mm_schedule s JOIN mm_member m ON m.team_id=s.team_id WHERE s.station_id=? AND m.user_id=? AND s.start_at<=? AND s.end_at>=? ORDER BY s.id LIMIT 1 FOR UPDATE",station,owner,start,end);require(!covering.isEmpty(),"任务时段必须被该工作站的人员排班完整覆盖");
        require(count("SELECT COUNT(*) FROM mm_task_slot s JOIN t_production_task t ON t.id=s.task_id WHERE s.task_id<>? AND t.task_status NOT IN(4,5) AND s.start_at<? AND s.end_at>? AND (s.station_id=? OR s.owner_id=?)",task,end,start,station,owner)==0,"工作站或负责人在该时段已有生产任务");
        Set<Long> equipment=rules.equipmentIds(station);for(var slot:db.queryForList("SELECT s.station_id FROM mm_task_slot s JOIN t_production_task t ON t.id=s.task_id WHERE s.task_id<>? AND t.task_status NOT IN(4,5) AND s.start_at<? AND s.end_at>?",task,end,start)){Set<Long> shared=rules.equipmentIds(id(slot.get("station_id")));shared.retainAll(equipment);require(shared.isEmpty(),"工作站共用设备在该时段已有生产任务");}
        var context=db.queryForMap("SELECT * FROM mm_task_context WHERE task_id=?",task);
        require(count("SELECT COUNT(*) FROM mm_task_slot s JOIN mm_task_context c ON c.task_id=s.task_id JOIN t_production_task t ON t.id=s.task_id WHERE c.work_order_id=? AND c.sequence_no<? AND t.task_status<>4 AND s.end_at>?",context.get("work_order_id"),context.get("sequence_no"),start)==0,"计划开始时间早于前序任务计划结束时间");
        require(count("SELECT COUNT(*) FROM mm_task_slot s JOIN mm_task_context c ON c.task_id=s.task_id WHERE c.work_order_id=? AND c.sequence_no>? AND s.start_at<?",context.get("work_order_id"),context.get("sequence_no"),end)==0,"计划结束时间晚于后序任务计划开始时间");
        db.update("INSERT INTO mm_task_slot(task_id,station_id,owner_id,start_at,end_at,quantity) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE station_id=VALUES(station_id),owner_id=VALUES(owner_id),start_at=VALUES(start_at),end_at=VALUES(end_at)",task,station,owner,start,end,t.get("quantity"));
        db.update("UPDATE mm_task_context SET station_id=?,schedule_id=? WHERE task_id=?",station,covering.get(0).get("id"),task);
        db.update("UPDATE t_production_task SET assigned_user_id=?,assigned_user_name=(SELECT real_name FROM t_user WHERE id=?),task_status=1 WHERE id=?",owner,owner,task);
    }
    public List<Map<String,Object>> planning(){return db.queryForList("SELECT t.id,t.task_no,t.quantity,t.product_id,t.task_status,t.assigned_user_id,t.assigned_user_name,c.station_id,c.operation_id,c.work_order_id,o.name operation_name,s.start_at,s.end_at,s.owner_id,w.name station_name FROM mm_task_context c JOIN t_production_task t ON t.id=c.task_id JOIN mm_operation o ON o.id=c.operation_id JOIN mm_station w ON w.id=c.station_id LEFT JOIN mm_task_slot s ON s.task_id=t.id ORDER BY COALESCE(s.start_at,'2999-01-01'),t.id");}
    @Transactional
    public int rotate(long rotation,long actor){
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");var p=db.queryForMap("SELECT * FROM mm_rotation WHERE id=? FOR UPDATE",rotation);require(id(p.get("is_generated"))==0,"此轮班计划已生成，不能重复生成");
        int shifts=switch(p.get("mode").toString()){case "DAY"->1;case "TWO"->2;case "THREE"->3;default->throw new IllegalArgumentException("轮班方式无效");};
        var teams=db.queryForList("SELECT team_id FROM mm_rotation_team WHERE rotation_id=? ORDER BY sequence_no",rotation);require(teams.size()>=shifts,"参与班组少于每日班次数");
        var shiftRows=db.queryForList("SELECT s.* FROM mm_rotation_shift a JOIN mm_shift s ON s.id=a.shift_id WHERE a.rotation_id=? ORDER BY a.sequence_no",rotation);require(shiftRows.size()==shifts,"请为此轮班计划配置对应数量的班次");
        // Select non-overlapping standard shifts and reject conflicting definitions through save().
        LocalDate start=LocalDate.parse(p.get("start_date").toString()),end=LocalDate.parse(p.get("end_date").toString());int created=0;
        for(LocalDate date=start;!date.isAfter(end);date=date.plusDays(1)){
            var overrides=db.queryForList("SELECT is_working FROM mm_holiday WHERE work_date=?",date);
            boolean working=overrides.isEmpty()?date.getDayOfWeek()!=DayOfWeek.SATURDAY&&date.getDayOfWeek()!=DayOfWeek.SUNDAY:id(overrides.get(0).get("is_working"))==1;
            if(!working)continue;
            long cycles=switch(p.get("cycle_unit").toString()){case "WEEK"->ChronoUnit.WEEKS.between(start,date);case "MONTH"->ChronoUnit.MONTHS.between(start,date);case "QUARTER"->ChronoUnit.MONTHS.between(start,date)/3;default->ChronoUnit.DAYS.between(start,date);};
            int offset=(int)(cycles/id(p.get("cycle_days"))%teams.size());
            for(int s=0;s<shifts;s++){var shift=shiftRows.get(s);LocalTime a=LocalTime.parse(shift.get("start_time").toString()),b=LocalTime.parse(shift.get("end_time").toString());LocalDateTime from=date.atTime(a),to=(b.isAfter(a)?date:date.plusDays(1)).atTime(b);
                Map<String,Object> data=new LinkedHashMap<>();data.put("team_id",teams.get((offset+s)%teams.size()).get("team_id"));data.put("shift_id",shift.get("id"));data.put("station_id",p.get("station_id"));data.put("start_at",from.toString());data.put("end_at",to.toString());
                foundation.save("schedules",null,data,actor);created++;
            }
        }
        db.update("UPDATE mm_rotation SET is_generated=1 WHERE id=?",rotation);return created;
    }
    public List<Map<String,Object>> calendar(long user,boolean all){return all?db.queryForList("SELECT s.*,w.name station_name,t.name team_name,m.user_id,u.real_name FROM mm_schedule s JOIN mm_station w ON w.id=s.station_id JOIN mm_team t ON t.id=s.team_id JOIN mm_member m ON m.team_id=t.id JOIN t_user u ON u.id=m.user_id ORDER BY start_at DESC LIMIT 2000"):db.queryForList("SELECT s.*,w.name station_name,t.name team_name FROM mm_schedule s JOIN mm_station w ON w.id=s.station_id JOIN mm_team t ON t.id=s.team_id JOIN mm_member m ON m.team_id=t.id WHERE m.user_id=? ORDER BY start_at DESC LIMIT 500",user);}
    public List<Map<String,Object>> checks(){return db.queryForList("SELECT d.*,p.code,p.name,p.standard,p.plan_type,p.owner_id,e.equipment_name,u.real_name owner_name FROM mm_check_due d JOIN mm_check_plan p ON p.id=d.plan_id JOIN t_equipment e ON e.id=p.equipment_id LEFT JOIN t_user u ON u.id=p.owner_id ORDER BY d.due_date DESC,d.id DESC LIMIT 500");}
    public List<Map<String,Object>> nonconformities(){return db.queryForList("SELECT n.*,i.code inspection_code,i.source_type,i.source_id,t.task_no,r.id rework_id,r.status rework_status FROM mm_nc n JOIN mm_inspection i ON i.id=n.inspection_id LEFT JOIN t_production_task t ON t.id=n.task_id LEFT JOIN mm_rework r ON r.nc_id=n.id ORDER BY n.id DESC");}
    @Transactional
    public void disposition(long nc,Map<String,Object> v,long actor){
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        var n=db.queryForMap("SELECT * FROM mm_nc WHERE id=? FOR UPDATE",nc);require("OPEN".equals(n.get("status")),"此不合格记录已处置");String decision=String.valueOf(v.get("decision")),reason=String.valueOf(v.getOrDefault("reason",""));require(!reason.isBlank()&&reason.length()<=500,"请填写处置依据，最多500字");
        require(Set.of("REWORK","REJECT","SCRAP").contains(decision),"处置方式必须为返工、拒收或报废");
        if(id(v.get("defect_id"))>0)require(count("SELECT COUNT(*) FROM mm_defect WHERE id=?",v.get("defect_id"))==1,"缺陷分类不存在");
        require(!decision.equals("REJECT")||id(n.get("task_id"))==0,"过程不合格请返工或整批报废，不能通过拒收解除工序约束");
        if(decision.equals("REWORK")){require(id(n.get("task_id"))>0,"非过程检验请使用拒收或报废处置");db.update("INSERT INTO mm_rework(nc_id,task_id) VALUES(?,?)",nc,n.get("task_id"));}
        db.update("UPDATE mm_nc SET status=?,decision=?,reason=?,approved_by=?,defect_id=? WHERE id=?",decision.equals("REWORK")?"REWORKING":decision.equals("SCRAP")&&id(n.get("task_id"))>0?"SCRAP_PENDING":"CLOSED",decision,reason,actor,id(v.get("defect_id"))==0?null:v.get("defect_id"),nc);
        if(decision.equals("SCRAP")&&id(n.get("task_id"))>0){
            // Scrap cannot be changed into a passing retest on the same finished quantity.
            db.update("UPDATE t_production_task SET task_status=5 WHERE id=?",n.get("task_id"));
            db.update("UPDATE mm_work_order SET status='SCRAP_HOLD' WHERE id=(SELECT work_order_id FROM mm_task_context WHERE task_id=?)",n.get("task_id"));
            db.update("UPDATE t_production_plan SET completed_quantity=0 WHERE id=(SELECT plan_id FROM t_production_task WHERE id=?)",n.get("task_id"));
        }
    }
    @Transactional
    public void executeScrap(long nc,long actor){
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");var record=db.queryForMap("SELECT * FROM mm_nc WHERE id=? FOR UPDATE",nc);
        if("CLOSED".equals(record.get("status"))&&"SCRAP".equals(record.get("decision")))return;
        require("SCRAP_PENDING".equals(record.get("status")),"该记录未批准整批报废");long wo=id(db.queryForMap("SELECT work_order_id FROM mm_task_context WHERE task_id=?",record.get("task_id")).get("work_order_id"));
        for(var stock:db.queryForList("SELECT s.* FROM mm_line_stock s JOIN t_material m ON m.id=s.material_id WHERE s.work_order_id=? AND s.quantity>0 AND m.material_type IN(2,3) FOR UPDATE",wo))lineStock.move(wo,id(stock.get("material_id")),id(stock.get("lot_id")),dec(stock.get("quantity")).negate(),"SCRAP",nc,actor);
        db.update("UPDATE mm_nc SET status='CLOSED' WHERE id=?",nc);db.update("INSERT INTO mm_audit(actor_id,resource,action,record_id) VALUES(?,'quality','SCRAP',?)",actor,nc);
        db.update("UPDATE mm_qc_todo SET status='CANCELLED',completed_at=NOW() WHERE task_id=?",record.get("task_id"));
    }
    @Transactional
    public void finishRework(long rework,String result,long actor){
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        require(result!=null&&!result.isBlank()&&result.length()<=500,"请填写实际返工内容，最多500字");var row=db.queryForMap("SELECT r.*,t.assigned_user_id FROM mm_rework r JOIN t_production_task t ON t.id=r.task_id WHERE r.id=? FOR UPDATE",rework);
        require("PENDING".equals(row.get("status")),"返工已完成");require(id(row.get("assigned_user_id"))==actor,"只有任务负责人可以提交返工结果");rules.equipmentAvailable(id(db.queryForMap("SELECT station_id FROM mm_task_context WHERE task_id=?",row.get("task_id")).get("station_id")));
        db.update("UPDATE mm_rework SET status='DONE',result=?,completed_by=?,completed_at=NOW() WHERE id=?",result,actor,rework);db.update("UPDATE mm_nc SET status='CLOSED' WHERE id=?",row.get("nc_id"));
    }
    private int noticeLimit(){var rows=db.queryForList("SELECT value FROM mm_parameter WHERE code='NOTICE_LIMIT'");return rows.isEmpty()?200:Math.toIntExact(Math.min(500,Math.max(1,id(rows.get(0).get("value")))));}
    public List<Map<String,Object>> notices(long user,long role){return db.queryForList("SELECT n.id,n.title,n.content,n.kind type,n.published_at createdAt,IF(r.id IS NULL,0,1) isRead FROM mm_notice n LEFT JOIN mm_notice_read r ON r.notice_id=n.id AND r.user_id=? WHERE (n.target_role IS NULL OR n.target_role=?) AND (n.expires_at IS NULL OR n.expires_at>NOW()) ORDER BY n.id DESC LIMIT ?",user,role,noticeLimit());}
    @Transactional
    public void publish(Map<String,Object> v,long actor){String title=String.valueOf(v.getOrDefault("title","")),content=String.valueOf(v.getOrDefault("content","")),kind=String.valueOf(v.getOrDefault("kind","info"));require(!title.isBlank()&&title.length()<=200&&!content.isBlank()&&content.length()<=10000,"公告标题1至200字，内容1至10000字");require(Set.of("info","warning","success","danger").contains(kind),"公告类型无效");Long role=id(v.get("target_role"))==0?null:id(v.get("target_role"));require(role==null||role>=1&&role<=11,"接收岗位无效");db.update("INSERT INTO mm_notice(title,content,kind,target_role,expires_at,actor_id) VALUES(?,?,?,?,?,?)",title,content,kind,role,v.get("expires_at"),actor);}
    @Transactional
    public void readNotice(long notice,long user,long role){require(count("SELECT COUNT(*) FROM mm_notice WHERE id=? AND (target_role IS NULL OR target_role=?) AND (expires_at IS NULL OR expires_at>NOW())",notice,role)>0,"公告不存在或不属于当前岗位");db.update("INSERT IGNORE INTO mm_notice_read(notice_id,user_id) VALUES(?,?)",notice,user);}
    public List<Map<String,Object>> sops(long task){return db.queryForList("SELECT s.id,s.name,s.version,s.image_path FROM mm_sop s JOIN mm_task_context c ON c.operation_id=s.operation_id JOIN t_production_task t ON t.id=c.task_id AND t.product_id=s.product_id WHERE c.task_id=? AND s.enabled=1 ORDER BY s.id DESC",task);}
    public Map<String,Object> monitor(){
        var runtime=Runtime.getRuntime();var os=ManagementFactory.getOperatingSystemMXBean();
        Map<String,Object> result=new LinkedHashMap<>();result.put("uptime_seconds",ManagementFactory.getRuntimeMXBean().getUptime()/1000);result.put("heap_used_bytes",runtime.totalMemory()-runtime.freeMemory());result.put("heap_max_bytes",runtime.maxMemory());result.put("available_processors",os.getAvailableProcessors());result.put("system_load_average",os.getSystemLoadAverage());result.put("database_time",db.queryForObject("SELECT NOW()",String.class));result.put("database_connections",db.queryForList("SHOW STATUS LIKE 'Threads_connected'"));result.put("pending_checks",count("SELECT COUNT(*) FROM mm_check_due WHERE status<>'DONE'"));result.put("automation_failures",count("SELECT COUNT(*) FROM mm_auto_event WHERE status='FAILED'"));result.put("scheduler_jobs",db.queryForList("SELECT code,name,enabled,interval_seconds,last_run FROM mm_job_config"));result.put("job_history",db.queryForList("SELECT code,status,started_at,finished_at,error FROM mm_job_run ORDER BY id DESC LIMIT 30"));result.put("business_counts",Map.of("work_orders",count("SELECT COUNT(*) FROM mm_work_order"),"warehouse_ledger_rows",count("SELECT COUNT(*) FROM t_inventory_transaction"),"line_ledger_rows",count("SELECT COUNT(*) FROM mm_line_movement"),"stock_documents",count("SELECT COUNT(*) FROM mm_document")));return result;
    }
    public List<Map<String,Object>> alerts(){return db.queryForList("SELECT m.id,m.material_code,m.material_name,m.safety_stock,m.max_stock,COALESCE(SUM(i.quantity),0) quantity,IF(COALESCE(SUM(i.quantity),0)<m.safety_stock,'LOW','HIGH') alert FROM t_material m LEFT JOIN t_inventory i ON i.material_id=m.id AND i.is_deleted=0 WHERE m.status=1 GROUP BY m.id HAVING quantity<m.safety_stock OR (m.max_stock IS NOT NULL AND quantity>m.max_stock)");}
}
