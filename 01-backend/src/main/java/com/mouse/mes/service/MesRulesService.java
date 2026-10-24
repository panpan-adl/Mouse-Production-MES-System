package com.mouse.mes.service;

import com.mouse.mes.entity.Maintenance;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Shared validations for the extended catalog and production resource gates. */
@Service
public class MesRulesService {
    private final JdbcTemplate db;
    private final MaintenanceService maintenance;
    private final TransactionTemplate tx;
    public MesRulesService(JdbcTemplate db, MaintenanceService maintenance, TransactionTemplate tx) {this.db=db;this.maintenance=maintenance;this.tx=tx;}
    static long id(Object v){return v==null?0:new BigDecimal(v.toString()).longValueExact();}
    static BigDecimal dec(Object v){return v==null?BigDecimal.ZERO:new BigDecimal(v.toString());}
    static void require(boolean ok,String message){if(!ok)throw new IllegalArgumentException(message);}
    private long count(String sql,Object...args){return Objects.requireNonNull(db.queryForObject(sql,Long.class,args));}

    @Transactional
    public String nextCode(String code){
        var rows=db.queryForList("SELECT * FROM mm_code_rule WHERE code=? FOR UPDATE",code);
        require(!rows.isEmpty(),"请先配置编码规则："+code);var r=rows.get(0);
        int width=Math.toIntExact(id(r.get("width")));long next=id(r.get("next_value"));
        require(width>=1&&width<=12&&next>0,"编码流水或位数设置无效");String candidate;
        for(int attempts=0;;attempts++){require(attempts<10000&&Long.toString(next).length()<=width,"编码流水已耗尽，请扩大位数或调整前缀");candidate=r.get("prefix")+String.format(Locale.ROOT,"%0"+width+"d",next++);if(!codeUsed(code,candidate))break;}
        db.update("UPDATE mm_code_rule SET next_value=? WHERE id=?",next,r.get("id"));return candidate;
    }
    private boolean codeUsed(String rule,String candidate){
        String[] resource=switch(rule){case "ITEM_CODE"->new String[]{"t_material","material_code"};case "PRODUCT_CODE"->new String[]{"t_product","product_code"};case "UOM_CODE"->new String[]{"t_uom","uom_code"};case "VENDOR_CODE"->new String[]{"t_supplier","supplier_code"};case "CLIENT_CODE"->new String[]{"t_customer","customer_code"};case "WORKSHOP_CODE"->new String[]{"mm_workshop","code"};case "WORKSTATION_CODE"->new String[]{"mm_station","code"};case "WAREHOUSE_CODE"->new String[]{"t_warehouse","warehouse_code"};case "AREA_CODE"->new String[]{"mm_area","code"};case "LOCATION_CODE"->new String[]{"mm_location","code"};case "WORKORDER_CODE"->new String[]{"mm_work_order","code"};case "PACKAGE_CODE"->new String[]{"mm_package","code"};case "PROCESS_CODE"->new String[]{"mm_operation","code"};case "ROUTE_CODE"->new String[]{"mm_route","code"};case "CHECKPLAN_CODE"->new String[]{"mm_check_plan","code"};case "MACHINERY_CODE"->new String[]{"t_equipment","equipment_code"};case "QC_TEMPLATR_CODE"->new String[]{"mm_qc_template","code"};case "QC_ITEM_CODE"->new String[]{"mm_qc_item","code"};case "DEFECT_CODE"->new String[]{"mm_defect","code"};case "BOM_CODE"->new String[]{"mm_bom","code"};case "TEAM_CODE"->new String[]{"mm_team","code"};case "CAL_PLAN_CODE"->new String[]{"mm_rotation","code"};case "SOP_CODE"->new String[]{"mm_sop","code"};case "REPAIR_CODE"->new String[]{"t_maintenance","maintenance_no"};case "QC_IQC_CODE","IPQC_CODE","OQC_CODE"->new String[]{"mm_inspection","code"};default->null;};
        if(resource!=null)return count("SELECT COUNT(*) FROM "+resource[0]+" WHERE "+resource[1]+"=?",candidate)>0;
        return count("SELECT COUNT(*) FROM mm_document WHERE code=?",candidate)+count("SELECT COUNT(*) FROM t_stock_in WHERE stock_in_no=?",candidate)+count("SELECT COUNT(*) FROM t_stock_out WHERE stock_out_no=?",candidate)>0;
    }
    public void defaults(String key,Map<String,Object> v){
        Map<String,Object> defaults=switch(key){
            case "units"->Map.of("conversion_rate",1);
            case "operations"->Map.of("is_key",0);
            case "qc-items"->Map.of("data_type","TEXT");
            case "qc-templates"->Map.of("phase","FINAL");
            case "checks"->Map.of("enabled",1,"plan_type","CHECK");
            case "rotations"->Map.of("cycle_unit","DAY");
            case "materials"->Map.of("sourcing_mode","AUTO");
            default->Map.of();};
        defaults.forEach((k,value)->{if(v.get(k)==null||v.get(k).toString().isBlank())v.put(k,value);});
    }
    public void validate(String key,Long record,Map<String,Object> v){
        long current=record==null?0:record;
        if(key.equals("categories")||key.equals("menus")){
            String table=key.equals("categories")?"mm_category":"mm_menu";long parent=id(v.get("parent_id"));Set<Long> seen=new HashSet<>();
            while(parent>0){require(parent!=current&&seen.add(parent),"父子关系不能形成循环");var row=db.queryForMap("SELECT * FROM "+table+" WHERE id=?",parent);if(key.equals("categories"))require(row.get("kind").equals(v.get("kind")),"父子分类必须属于相同类型");parent=id(row.get("parent_id"));}
        }
        if(key.equals("units")){
            long main=id(v.get("main_unit_id"));require(dec(v.get("conversion_rate")).signum()>0,"换算率必须大于0");
            if(main==0)require(dec(v.get("conversion_rate")).compareTo(BigDecimal.ONE)==0,"主单位换算率必须为1");
            else require(main!=current&&count("SELECT COUNT(*) FROM t_uom WHERE id=? AND main_unit_id IS NULL AND is_deleted=0",main)==1,"辅单位必须引用有效主单位，不能引用自身或其他辅单位");
            if(record!=null)require(count("SELECT COUNT(*) FROM t_uom WHERE main_unit_id=?",record)==0||main==0,"已被辅单位引用的主单位不能改为辅单位");
            if(record!=null){var old=db.queryForMap("SELECT * FROM t_uom WHERE id=?",record);if(id(old.get("main_unit_id"))!=main||dec(old.get("conversion_rate")).compareTo(dec(v.get("conversion_rate")))!=0)require(count("SELECT COUNT(*) FROM mm_document_line WHERE input_unit_id=?",record)==0&&count("SELECT COUNT(*) FROM t_inventory_transaction t JOIN t_material m ON m.id=t.material_id WHERE m.unit=?",old.get("uom_name"))==0,"单位已用于库存流水，不能修改历史换算含义");}
        }
        if((key.equals("materials")||key.equals("products"))&&id(v.get("category_id"))>0){String expected=key.equals("products")||id(v.get("material_type"))>1?"PRODUCT":"MATERIAL";require(count("SELECT COUNT(*) FROM mm_category WHERE id=? AND kind=?",v.get("category_id"),expected)==1,"分类类型与物料/产品不匹配");}
        if(key.equals("materials")&&v.get("max_stock")!=null)require(dec(v.get("max_stock")).compareTo(dec(v.get("safety_stock")))>=0,"最大库存不得低于安全库存");
        if(key.equals("qc-items")){
            if(record!=null)require(count("SELECT COUNT(*) FROM mm_qc_line l JOIN mm_inspection i ON i.template_id=l.template_id WHERE l.item_id=?",record)==0,"该检测项已有使用记录，请新建检测项并建立模板版本");
            if(record!=null)require(count("SELECT COUNT(*) FROM mm_qc_line l JOIN mm_document_check i ON i.template_id=l.template_id WHERE l.item_id=?",record)==0,"该检测项已有批次检验记录，请新建版本");
            if("NUMBER".equals(v.get("data_type")))require(v.get("lower_limit")!=null&&v.get("upper_limit")!=null&&dec(v.get("lower_limit")).compareTo(dec(v.get("upper_limit")))<=0,"数值检测项必须设置有效的上下限");
        }
        if(key.equals("qc-templates")){
            if("PROCESS".equals(v.get("kind")))require(id(v.get("operation_id"))>0&&count("SELECT COUNT(*) FROM mm_operation o JOIN mm_route r ON r.id=o.route_id WHERE o.id=? AND r.product_id=?",v.get("operation_id"),v.get("product_id"))==1,"过程检验模板必须绑定该产品的工序");
            else require(v.get("operation_id")==null,"来料和出货模板不应绑定生产工序");
        }
        if(key.equals("operations")&&id(v.get("is_key"))==1)require(count("SELECT COUNT(*) FROM mm_operation WHERE route_id=? AND is_key=1 AND id<>?",v.get("route_id"),current)==0,"一条路线只能指定一个关键工序");
        if(key.equals("station-resources")){
            String type=String.valueOf(v.get("resource_type"));
            require(!type.equals("EQUIPMENT")||id(v.get("equipment_id"))>0,"请选择资源设备");
            require(!type.equals("USER")||id(v.get("user_id"))>0,"请选择资源人员");
            require(!type.equals("TOOL")||v.get("name")!=null,"请填写工装名称");
        }
        if(key.equals("consumption")){
            require(count("SELECT COUNT(*) FROM mm_task_context WHERE operation_id=?",v.get("operation_id"))==0,"工序已经下发，请新建路线版本后调整耗料");
            require(count("SELECT COUNT(*) FROM mm_task_consumption WHERE operation_id=? AND material_id=? AND id<>?",v.get("operation_id"),v.get("material_id"),current)==0,"同一工序物料不能重复配置");
        }
        if(key.equals("sops")){
            require(count("SELECT COUNT(*) FROM mm_operation o JOIN mm_route r ON r.id=o.route_id WHERE o.id=? AND r.product_id=?",v.get("operation_id"),v.get("product_id"))==1,"SOP工序必须属于所选产品");
            require(String.valueOf(v.get("image_path")).matches("/api/mes/sop-images/[a-f0-9-]+\\.(png|jpg|webp)"),"请先上传SOP图片");
            java.nio.file.Path root=java.nio.file.Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();if(root.getFileName().toString().equals("01-backend"))root=root.getParent();String file=v.get("image_path").toString().substring(v.get("image_path").toString().lastIndexOf('/')+1);require(java.nio.file.Files.isRegularFile(root.resolve(".runtime/sop-images").resolve(file)),"SOP图片不存在，请重新上传");
        }
        if(key.equals("code-rules")){
            require(String.valueOf(v.get("prefix")).matches("[A-Za-z0-9_.-]{1,20}"),"编码前缀请使用1至20位字母、数字、点、横线或下划线");
            require(id(v.get("width"))>=1&&id(v.get("width"))<=12&&id(v.get("next_value"))>0,"编码位数应为1至12，流水号必须为正整数");
            if(record!=null)require(id(v.get("next_value"))>=id(db.queryForMap("SELECT next_value FROM mm_code_rule WHERE id=?",record).get("next_value")),"不能回退编码流水号");
        }
        if(key.equals("menus"))require(String.valueOf(v.get("path")).startsWith("/")&&!String.valueOf(v.get("path")).startsWith("//"),"菜单必须指向系统内页面");
        if(key.equals("parameters"))require(!String.valueOf(v.get("code")).toLowerCase(Locale.ROOT).matches(".*(password|secret|token|key).*"),"系统参数不能存放凭据或密钥");
        if(key.equals("parameters")&&Set.of("NOTICE_LIMIT","DEFAULT_DURATION_MINUTES").contains(v.get("code")))require(id(v.get("value"))>0&&id(v.get("value"))<=500,"此参数必须为1至500的整数");
        if(key.equals("parameters")&&v.get("code").equals("APP_NAME"))require(v.get("value").toString().length()<=80,"系统名称不得超过80字");
        if(key.equals("check-records"))require(!((LocalDateTime)v.get("checked_at")).isAfter(LocalDateTime.now().plusMinutes(5)),"不能提前登记未来点检结果");
        if(key.equals("jobs")){require(Set.of("EQUIPMENT_CHECK","REPORT_QC").contains(v.get("code")),"只能配置系统支持的业务任务，不能执行任意脚本");long seconds=id(v.get("interval_seconds"));require(seconds>=(v.get("code").equals("REPORT_QC")?2:60)&&seconds<=86400,"调度间隔超出支持范围");if(record!=null)require(v.get("code").equals(db.queryForMap("SELECT code FROM mm_job_config WHERE id=?",record).get("code")),"不能修改任务标识");}
        if(key.equals("rotations")){
            LocalDate start=LocalDate.parse(v.get("start_date").toString()),end=LocalDate.parse(v.get("end_date").toString());
            require(!end.isBefore(start)&&!end.isAfter(start.plusDays(365)),"轮班计划需在一年以内且结束日期不早于开始日期");
            require(id(v.get("cycle_days"))>=1&&id(v.get("cycle_days"))<=366,"轮班周期为1至366天");
            if(record!=null)require(count("SELECT COUNT(*) FROM mm_rotation WHERE id=? AND is_generated=1",record)==0,"已生成轮班不可直接修改，请新建计划");
        }
        if(key.equals("rotation-teams")||key.equals("rotation-shifts"))require(count("SELECT COUNT(*) FROM mm_rotation WHERE id=? AND is_generated=0",v.get("rotation_id"))==1,"已生成的轮班计划不能修改参与班组或班次");
        if(key.equals("positions"))require(count("SELECT COUNT(*) FROM t_department WHERE id=?",v.get("department_id"))==1,"岗位所属部门不存在");
        if(key.equals("role-menus"))require(count("SELECT COUNT(*) FROM t_role WHERE id=?",v.get("role_id"))==1,"角色不存在");
    }
    @Transactional
    public long fault(long plan,long record,String description){
        var p=db.queryForMap("SELECT * FROM mm_check_plan WHERE id=?",plan);long equipment=id(p.get("equipment_id"));
        db.queryForList("SELECT id FROM t_equipment WHERE id=? FOR UPDATE",equipment);
        var unfinished=db.queryForList("SELECT id FROM t_maintenance WHERE equipment_id=? AND status IN ('PENDING','PROCESSING') ORDER BY id LIMIT 1",equipment);
        long repair;
        if(!unfinished.isEmpty())repair=id(unfinished.get(0).get("id"));
        else {Maintenance m=new Maintenance();m.setMaintenanceNo(nextCode("REPAIR_CODE"));m.setEquipmentId(equipment);m.setMaintenanceType(2);m.setFaultDesc(description);m.setRemark("来源点检记录 "+record);repair=maintenance.create(m).getId();}
        db.update("UPDATE t_equipment SET status=IF(status='REPAIRING','REPAIRING','REPAIR_REQUESTED') WHERE id=?",equipment);
        return repair;
    }
    public void equipmentAvailable(long station){
        require(count("SELECT COUNT(*) FROM mm_station WHERE id=?",station)==1,"工作站不存在");
        require(count("SELECT COUNT(*) FROM t_equipment e WHERE (e.id IN(SELECT equipment_id FROM mm_station WHERE id=?) OR e.id IN(SELECT equipment_id FROM mm_station_resource WHERE station_id=? AND resource_type='EQUIPMENT')) AND (e.status NOT IN ('NORMAL','REPAIRED') OR e.is_deleted<>0 OR EXISTS(SELECT 1 FROM t_maintenance m WHERE m.equipment_id=e.id AND m.status IN('PENDING','PROCESSING')))",station,station)==0,"工作站设备不可用，请先维修完成或调整排产");
    }
    public Set<Long> equipmentIds(long station){
        Set<Long> ids=new HashSet<>();for(var row:db.queryForList("SELECT equipment_id FROM mm_station WHERE id=? AND equipment_id IS NOT NULL UNION SELECT equipment_id FROM mm_station_resource WHERE station_id=? AND resource_type='EQUIPMENT' AND equipment_id IS NOT NULL",station,station))ids.add(id(row.get("equipment_id")));return ids;
    }
    public BigDecimal convert(long material,long unit,BigDecimal quantity){
        if(unit==0)return quantity;
        var materialRow=db.queryForMap("SELECT unit FROM t_material WHERE id=?",material);var target=db.queryForMap("SELECT * FROM t_uom WHERE uom_name=? AND is_deleted=0",materialRow.get("unit"));var source=db.queryForMap("SELECT * FROM t_uom WHERE id=? AND is_deleted=0",unit);
        long sourceRoot=id(source.get("main_unit_id"))==0?id(source.get("id")):id(source.get("main_unit_id")),targetRoot=id(target.get("main_unit_id"))==0?id(target.get("id")):id(target.get("main_unit_id"));require(sourceRoot==targetRoot,"录入单位与物料基本单位不能换算");
        try{return quantity.multiply(dec(source.get("conversion_rate"))).divide(dec(target.get("conversion_rate")),2,java.math.RoundingMode.UNNECESSARY);}catch(ArithmeticException ex){throw new IllegalArgumentException("换算后的库存数量精度超过两位小数，请调整单位或数量");}
    }
    public void taskGate(long task){
        var contexts=db.queryForList("SELECT * FROM mm_task_context WHERE task_id=?",task);if(contexts.isEmpty())return;
        require(count("SELECT COUNT(*) FROM mm_work_order WHERE id=? AND status='SCRAP_HOLD'",contexts.get(0).get("work_order_id"))==0,"工单存在报废处置，已冻结后续生产");
        long station=id(contexts.get(0).get("station_id"));equipmentAvailable(station);
        require(count("SELECT COUNT(*) FROM mm_nc WHERE task_id=? AND status<>'CLOSED'",task)==0,"任务存在未完成的不合格处置，请先返工或审批");
        var slots=db.queryForList("SELECT * FROM mm_task_slot WHERE task_id=?",task);
        require(!slots.isEmpty(),"任务尚未排产，请先分配工作站、负责人和生产时间");var slot=slots.get(0);
        LocalDateTime now=LocalDateTime.now();LocalDateTime start=LocalDateTime.parse(slot.get("start_at").toString().replace(' ','T')),end=LocalDateTime.parse(slot.get("end_at").toString().replace(' ','T'));
        require(!now.isBefore(start)&&now.isBefore(end),"当前不在任务计划生产时间内，请调整排产后开工/报工");
        require(count("SELECT COUNT(*) FROM mm_schedule s JOIN mm_member m ON m.team_id=s.team_id WHERE s.station_id=? AND m.user_id=? AND s.start_at<=? AND s.end_at>=?",station,slot.get("owner_id"),start,end)>0,"负责人没有覆盖该任务时段的工作站排班");
        require(count("SELECT COUNT(*) FROM t_user WHERE id=? AND deleted=0 AND status<>0",slot.get("owner_id"))==1,"任务负责人已离职或停用");
    }
    @Scheduled(fixedDelay=60000,initialDelay=15000)
    public void periodicChecks(){
        // One transaction and unique (plan,date) protect against simultaneous worker instances.
        try {tx.executeWithoutResult(s->{
            db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
            if(count("SELECT COUNT(*) FROM mm_job_config WHERE code='EQUIPMENT_CHECK' AND enabled=1 AND (last_run IS NULL OR TIMESTAMPDIFF(SECOND,last_run,NOW())>=interval_seconds)")==0)return;
            var plans=db.queryForList("SELECT p.*,DATE(p.created_at) anchor FROM mm_check_plan p WHERE enabled=1 AND interval_days>0");
            for(var p:plans){LocalDate anchor=LocalDate.parse(p.get("anchor").toString());LocalDate today=LocalDate.now();long period=id(p.get("interval_days"));Object last=db.queryForObject("SELECT MAX(due_date) FROM mm_check_due WHERE plan_id=?",Object.class,p.get("id"));LocalDate due=last==null?anchor:LocalDate.parse(last.toString()).plusDays(period);int generated=0;while(!due.isAfter(today)&&generated++<366){db.update("INSERT IGNORE INTO mm_check_due(plan_id,due_date) VALUES(?,?)",p.get("id"),due);due=due.plusDays(period);}}
            db.update("UPDATE mm_check_due SET status='OVERDUE' WHERE status='PENDING' AND due_date<CURDATE()");
            db.update("UPDATE mm_job_config SET last_run=NOW() WHERE code='EQUIPMENT_CHECK'");db.update("INSERT INTO mm_job_run(code,status,finished_at) VALUES('EQUIPMENT_CHECK','SUCCEEDED',NOW())");
        });}catch(org.springframework.dao.DataAccessException ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("点检调度未完成，请检查MES扩展迁移：{}",ex.getClass().getSimpleName());}
    }
    public void recordedCheck(Map<String,Object> v,long record){
        db.update("UPDATE mm_check_due SET status='DONE',record_id=? WHERE id=(SELECT id FROM(SELECT id FROM mm_check_due WHERE plan_id=? AND status IN ('PENDING','OVERDUE') AND due_date<=DATE(?) ORDER BY due_date LIMIT 1)x)",record,v.get("plan_id"),v.get("checked_at"));
    }
}
