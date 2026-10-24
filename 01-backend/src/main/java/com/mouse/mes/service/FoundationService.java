package com.mouse.mes.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stage-one master data and canonical manufacturing workflow. SQL identifiers come only from the bundled catalog. */
@Service
public class FoundationService {
    @org.springframework.beans.factory.annotation.Autowired
    private AutomationService automation;
    @org.springframework.beans.factory.annotation.Autowired
    private MesRulesService mesRules;
    @org.springframework.beans.factory.annotation.Autowired
    private LineStockService lineStock;
    public record Field(String name, String label, String type, String ref, boolean required, Map<String,String> options) {}
    public record Resource(String key, String label, String table, List<Field> fields, List<Integer> roles,
                           String unique, String where, Map<String,Object> extra) {}
    private final JdbcTemplate db;
    private final ObjectMapper json;
    @org.springframework.beans.factory.annotation.Autowired
    private FulfillmentService fulfillment;
    private final Map<String,Resource> catalog = new LinkedHashMap<>();
    public FoundationService(JdbcTemplate db, ObjectMapper json) throws Exception {
        this.db=db; this.json=json;
        try (var input=new ClassPathResource("stage1-catalog.json").getInputStream()) {
            List<Resource> definitions=json.readValue(input,new TypeReference<List<Resource>>(){});
            for (Resource definition:definitions) catalog.put(definition.key(),definition);
        }
    }
    public List<Resource> catalog() { return new ArrayList<>(catalog.values()); }
    public Resource definition(String key) {
        Resource resource=catalog.get(key);
        if(resource==null) throw new IllegalArgumentException("不支持的配置类型");
        return resource;
    }
    public List<Map<String,Object>> list(String key) {
        if(key.equals("users")) return db.queryForList("SELECT id,real_name AS name,user_code FROM t_user WHERE deleted=0 AND status<>0");
        if(key.equals("departments"))return db.queryForList("SELECT id,dept_name name FROM t_department WHERE status=1");
        if(key.equals("roles"))return db.queryForList("SELECT id,role_name name FROM t_role");
        if(key.equals("check-records"))return db.queryForList("SELECT r.id,r.plan_id,r.checked_at,r.actual_value,r.result,r.remark,r.maintenance_id,m.maintenance_no FROM mm_check_record r LEFT JOIN t_maintenance m ON m.id=r.maintenance_id ORDER BY r.id DESC");
        Resource r=definition(key);
        return db.queryForList("SELECT id,"+String.join(",",r.fields().stream().map(Field::name).toList())+" FROM "+r.table()+" WHERE "+r.where()+" ORDER BY id DESC");
    }
    private boolean isGeneratedCode(String key,Field f){return !Set.of("code-rules","parameters","menus","jobs").contains(key)&&f.ref()==null&&(f.name().equals("code")||f.name().endsWith("_code"));}
    private String generatedCode(String key){
        String rule=switch(key){case "units"->"UOM_CODE";case "materials"->"ITEM_CODE";case "products"->"PRODUCT_CODE";case "suppliers"->"VENDOR_CODE";case "customers"->"CLIENT_CODE";case "workshops"->"WORKSHOP_CODE";case "stations"->"WORKSTATION_CODE";case "warehouses"->"WAREHOUSE_CODE";case "areas"->"AREA_CODE";case "locations"->"LOCATION_CODE";case "operations"->"PROCESS_CODE";case "routes"->"ROUTE_CODE";case "checks"->"CHECKPLAN_CODE";case "equipment"->"MACHINERY_CODE";case "qc-templates"->"QC_TEMPLATR_CODE";case "qc-items"->"QC_ITEM_CODE";case "defects"->"DEFECT_CODE";case "bom-versions"->"BOM_CODE";case "teams"->"TEAM_CODE";case "rotations"->"CAL_PLAN_CODE";case "sops"->"SOP_CODE";default->null;};
        return rule==null?UUID.randomUUID().toString().replace("-","").substring(0,20):mesRules.nextCode(rule);
    }
    public Map<String,Object> get(String key,long id) {
        return list(key).stream().filter(row->number(row.get("id"))==id).findFirst().orElseThrow(()->new IllegalArgumentException("记录不存在或已停用"));
    }
    private long insert(String table,Map<String,Object> data) {
        String sql="INSERT INTO "+table+" ("+String.join(",",data.keySet())+") VALUES ("+String.join(",",Collections.nCopies(data.size(),"?"))+")";
        GeneratedKeyHolder holder=new GeneratedKeyHolder();
        db.update(connection->{PreparedStatement statement=connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);int index=1;for(Object value:data.values())statement.setObject(index++,value);return statement;},holder);
        return Objects.requireNonNull(holder.getKey()).longValue();
    }
    private void audit(long actor,String key,String action,long id) {
        db.update("INSERT INTO mm_audit(actor_id,resource,action,record_id) VALUES(?,?,?,?)",actor,key,action,id);
    }
    public static long number(Object value) { return value==null?0: new BigDecimal(value.toString()).longValueExact(); }
    private static BigDecimal decimal(Object value) { return new BigDecimal(value.toString()); }
    private static String text(Object value) { return value==null?"":value.toString().trim(); }
    private int count(String sql,Object... args) { return Objects.requireNonNull(db.queryForObject(sql,Integer.class,args)); }
    private void fail(boolean condition,String message) { if(condition) throw new IllegalArgumentException(message); }
    private Object normalized(Field field,Object value) {
        if(value==null || text(value).isEmpty()) { fail(field.required(),field.label()+"不能为空");return null; }
        String t=text(value);fail(t.length()>500,"输入过长");
        if(field.ref()!=null) { long id=number(value);get(field.ref(),id);return id; }
        if(field.options()!=null) { fail(!field.options().containsKey(t),field.label()+"取值无效");return t; }
        if(field.type().equals("decimal")) return decimal(value);
        if(field.type().equals("date")) return java.time.LocalDate.parse(t);
        if(field.type().equals("positive")||field.type().equals("number")) {
            BigDecimal n=decimal(value);fail(n.signum()<0 || field.type().equals("positive")&&n.signum()==0,field.label()+"数量无效");
            if(Set.of("sequence_no","interval_days","width","next_value","cycle_days","interval_seconds","x_coordinate","y_coordinate","z_coordinate").contains(field.name()))fail(n.stripTrailingZeros().scale()>0,field.label()+"必须是整数");return n;
        }
        if(field.type().equals("datetime-local")) return LocalDateTime.parse(t.replace(' ','T'));
        if(field.type().equals("time")) { java.time.LocalTime.parse(t);return t; }
        return t;
    }
    @Transactional
    public Map<String,Object> save(String key,Long id,Map<String,Object> input,long actor) {
        fulfillment.lock();
        input=new LinkedHashMap<>(input);mesRules.defaults(key,input);
        Resource r=definition(key);Map<String,Object> values=new LinkedHashMap<>();
        for(Field f:r.fields())if(isGeneratedCode(key,f)) {
            if(id!=null)input.put(f.name(),get(key,id).get(f.name()));
            else input.put(f.name(),generatedCode(key));
        }
        if(id!=null)get(key,id);
        fail(id!=null && key.equals("check-records"),"点检记录不可修改，请新增记录保留历史");
        for(Field f:r.fields())values.put(f.name(),normalized(f,input.get(f.name())));
        if(r.unique()!=null) fail(count("SELECT COUNT(*) FROM "+r.table()+" WHERE "+r.unique()+"=? AND id<>?",values.get(r.unique()),id==null?0:id)>0,"编码已存在，请使用其他编码");
        if(key.equals("maintenance-plans")) {
            fail(number(values.get("frequency"))<1||number(values.get("frequency"))>1000,"保养间隔须为1至1000的整数");
            fail(new BigDecimal(values.get("frequency").toString()).stripTrailingZeros().scale()>0,"保养间隔必须是整数");
            fail(number(values.get("remind_days"))>365||new BigDecimal(values.get("remind_days").toString()).stripTrailingZeros().scale()>0,"提前提醒应为0至365天的整数");
            fail(count("SELECT COUNT(*) FROM t_user WHERE id=? AND role_id=8 AND status=1 AND deleted=0",values.get("owner_id"))!=1,"负责人须为在职设备维护员");
            if(id!=null) { var old=get(key,id); fail(count("SELECT COUNT(*) FROM mm_maintenance_occurrence WHERE plan_id=?",id)>0&&(!PeriodicMaintenanceService.dateTime(old.get("anchor_at")).equals(values.get("anchor_at"))||number(old.get("frequency"))!=number(values.get("frequency"))||!old.get("cycle_unit").equals(values.get("cycle_unit"))),"已有保养任务的周期不可改变，请停用原计划后新建"); }
        }
        validateBusiness(key,id,values);
        mesRules.validate(key,id,values);
        if(id==null) { values.putAll(r.extra());if(key.equals("check-records"))values.put("actor_id",actor);id=insert(r.table(),values); }
        else { final List<Object> args=new ArrayList<>(values.values());args.add(id);db.update("UPDATE "+r.table()+" SET "+String.join(",",values.keySet().stream().map(n->n+"=?").toList())+" WHERE id=?",args.toArray()); }
        if(key.equals("locations")) {
            long warehouse=number(db.queryForMap("SELECT warehouse_id FROM mm_area WHERE id=?",values.get("area_id")).get("warehouse_id"));
            var matches=db.queryForList("SELECT id FROM t_location WHERE warehouse_id=? AND location_code=?",warehouse,values.get("code"));
            if(matches.isEmpty()) db.update("INSERT INTO t_location(warehouse_id,location_code,location_name,status,is_deleted) VALUES(?,?,?,'1',0)",warehouse,values.get("code"),values.get("name"));
            else db.update("UPDATE t_location SET location_name=? WHERE id=?",values.get("name"),matches.get(0).get("id"));
        }
        if(key.equals("check-records") && text(values.get("result")).equals("FAIL")) {
            Map<String,Object> plan=get("checks",number(values.get("plan_id")));Map<String,Object> equipment=get("equipment",number(plan.get("equipment_id")));
            long maintenance=mesRules.fault(number(values.get("plan_id")),id,text(values.get("actual_value")));
            db.update("UPDATE mm_check_record SET maintenance_id=? WHERE id=?",maintenance,id);
        }
        if(key.equals("check-records"))mesRules.recordedCheck(values,id);
        if(key.equals("sops")&&number(values.get("enabled"))==1)db.update("UPDATE mm_sop SET enabled=0 WHERE product_id=? AND operation_id=? AND id<>?",values.get("product_id"),values.get("operation_id"),id);
        audit(actor,key,"SAVE",id);return get(key,id);
    }
    private void validateBusiness(String key,Long id,Map<String,Object> v) {
        long excluded=id==null?0:id;
        if(id!=null && key.equals("bom-lines")) fail(count("SELECT COUNT(*) FROM mm_work_order WHERE bom_id=?",get(key,id).get("bom_id"))>0,"原BOM已使用，不能移动或修改组成");
        if(id!=null && key.equals("qc-lines")) fail(count("SELECT COUNT(*) FROM mm_inspection WHERE template_id=?",get(key,id).get("template_id"))>0,"原检测模板已使用，不能移动检测项");
        if(id!=null && key.equals("mappings")) fail(count("SELECT COUNT(*) FROM mm_bom WHERE product_id=?",get(key,id).get("product_id"))>0,"产品已有BOM，映射不能修改");
        if(id!=null && key.equals("members")) fail(count("SELECT COUNT(*) FROM mm_schedule WHERE team_id=?",get(key,id).get("team_id"))>0,"原班组已有排班，不能直接移动成员");
        if(key.equals("locations") && id!=null) fail(!Objects.equals(get(key,id).get("code"),v.get("code")) || number(get(key,id).get("area_id"))!=number(v.get("area_id")),"库位编码与库区不可直接修改，请新增库位");
        if(key.equals("bom-versions")) fail(count("SELECT COUNT(*) FROM mm_product_material WHERE product_id=? AND material_id=?",v.get("product_id"),v.get("root_material_id"))==0,"BOM根节点必须是产品映射的成品物料");
        if(key.equals("materials")||key.equals("products")) fail(count("SELECT COUNT(*) FROM t_uom WHERE uom_name=? AND is_deleted=0",v.get("unit"))==0,"请先维护有效计量单位");
        if(key.equals("units")&&id!=null) {
            String old=text(get(key,id).get("uom_name"));
            fail(!old.equals(v.get("uom_name"))&&(count("SELECT COUNT(*) FROM t_material WHERE unit=?",old)+count("SELECT COUNT(*) FROM t_product WHERE unit=?",old)>0),"已引用单位不能改名");
        }
        if(key.equals("mappings")) fail(!Set.of(2L,3L).contains(number(get("materials",number(v.get("material_id"))).get("material_type"))),"产品必须映射半成品或成品物料");
        if(key.equals("members")) {
            fail(count("SELECT COUNT(*) FROM mm_member WHERE team_id=? AND user_id=? AND id<>?",v.get("team_id"),v.get("user_id"),excluded)>0,"该成员已在班组中");
            fail(count("SELECT COUNT(*) FROM mm_schedule WHERE team_id=?",v.get("team_id"))>0,"班组已排班，调整成员前请先删除未来排班");
        }
        if(key.equals("schedules")) validateSchedule(excluded,v);
        if(key.equals("schedules")&&id!=null) fail(count("SELECT COUNT(*) FROM mm_task_context WHERE schedule_id=?",id)>0,"已用于工单的排班不可修改");
        if(key.equals("operations")) {
            fail(count("SELECT COUNT(*) FROM mm_work_order WHERE route_id=?",v.get("route_id"))>0,"路线已下发，请新建版本后维护工序");
            fail(count("SELECT COUNT(*) FROM mm_operation WHERE route_id=? AND sequence_no=? AND id<>?",v.get("route_id"),v.get("sequence_no"),excluded)>0,"路线中工序顺序重复");
            if(v.get("predecessor_id")!=null) {Map<String,Object> p=get("operations",number(v.get("predecessor_id")));fail(number(p.get("id"))==excluded||number(p.get("route_id"))!=number(v.get("route_id"))||decimal(p.get("sequence_no")).compareTo(decimal(v.get("sequence_no")))>=0,"前置工序必须来自同一路线且顺序在前");}
            if(id!=null) fail(count("SELECT COUNT(*) FROM mm_task_context WHERE operation_id=?",id)>0,"工序已下发，修改请建立新路线版本");
        }
        if(key.equals("bom-lines")) {
            fail(v.get("parent_material_id").equals(v.get("material_id")),"物料不能包含自身");
            fail(number(get("materials",number(v.get("parent_material_id"))).get("material_type"))==1,"原材料不能作为父节点");
            fail(count("SELECT COUNT(*) FROM mm_work_order WHERE bom_id=?",v.get("bom_id"))>0,"BOM已冻结到工单，请新建版本");
            List<Map<String,Object>> lines=db.queryForList("SELECT * FROM mm_bom_line WHERE bom_id=? AND id<>?",v.get("bom_id"),excluded);lines.add(v);checkCycles(lines);
        }
        if(key.equals("bom-versions")&&id!=null) fail(count("SELECT COUNT(*) FROM mm_work_order WHERE bom_id=?",id)>0,"BOM已使用，请创建新版本");
        if(key.equals("routes")&&id!=null) fail(count("SELECT COUNT(*) FROM mm_work_order WHERE route_id=?",id)>0,"路线已使用，请创建新版本");
        if(key.equals("qc-lines")) fail(count("SELECT COUNT(*) FROM mm_inspection WHERE template_id=?",v.get("template_id"))>0,"模板已有检验记录，请新建模板版本");
        if(key.equals("qc-lines"))fail(count("SELECT COUNT(*) FROM mm_document_check WHERE template_id=?",v.get("template_id"))>0,"模板已有批次检验记录，请新建版本");
        if(key.equals("qc-templates")&&id!=null) fail(count("SELECT COUNT(*) FROM mm_inspection WHERE template_id=?",id)>0,"模板已使用，请创建新版本");
        if(key.equals("qc-templates")&&id!=null)fail(count("SELECT COUNT(*) FROM mm_document_check WHERE template_id=?",id)>0,"模板已有批次检验记录，请创建新版本");
    }
    private void validateSchedule(long excluded,Map<String,Object> v) {
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        fail(((LocalDateTime)v.get("end_at")).compareTo((LocalDateTime)v.get("start_at"))<=0,"结束时间必须晚于开始时间（跨日请填写次日）");
        fail(count("SELECT COUNT(*) FROM mm_member WHERE team_id=?",v.get("team_id"))==0,"班组尚未设置成员");
        fail(count("SELECT COUNT(*) FROM mm_member m JOIN t_user u ON u.id=m.user_id WHERE m.team_id=? AND (u.status=0 OR u.deleted=1)",v.get("team_id"))>0,"班组包含离职人员，请先调整班组成员");
        Map<String,Object> station=get("stations",number(v.get("station_id")));
        mesRules.equipmentAvailable(number(v.get("station_id")));
        if(v.get("equipment_id")==null&&station.get("equipment_id")!=null)v.put("equipment_id",station.get("equipment_id"));
        for(Map<String,Object> existing:db.queryForList("SELECT * FROM mm_schedule WHERE id<>? AND start_at<? AND end_at>? FOR UPDATE",excluded,v.get("end_at"),v.get("start_at"))) {
            boolean people=count("SELECT COUNT(*) FROM mm_member a JOIN mm_member b ON a.user_id=b.user_id WHERE a.team_id=? AND b.team_id=?",v.get("team_id"),existing.get("team_id"))>0;
            Set<Long> resources=mesRules.equipmentIds(number(v.get("station_id"))),other=mesRules.equipmentIds(number(existing.get("station_id")));if(v.get("equipment_id")!=null)resources.add(number(v.get("equipment_id")));if(existing.get("equipment_id")!=null)other.add(number(existing.get("equipment_id")));resources.retainAll(other);
            fail(people||number(v.get("station_id"))==number(existing.get("station_id"))||!resources.isEmpty(),"人员、工作站或设备在该时间段已有排班");
        }
    }
    private void checkCycles(List<Map<String,Object>> lines) {
        Map<Long,List<Long>> graph=new HashMap<>();for(var l:lines)graph.computeIfAbsent(number(l.get("parent_material_id")),k->new ArrayList<>()).add(number(l.get("material_id")));
        for(long node:graph.keySet())visit(node,graph,new HashSet<>(),new HashSet<>());
    }
    private void visit(long node,Map<Long,List<Long>> graph,Set<Long> active,Set<Long> done) {
        fail(active.contains(node),"BOM存在循环引用");if(done.contains(node))return;active.add(node);
        for(long child:graph.getOrDefault(node,List.of()))visit(child,graph,active,done);
        active.remove(node);done.add(node);
    }
    @Transactional
    public void delete(String key,long id,long actor) {
        fulfillment.lock();
        fail(key.equals("jobs"),"系统任务不可删除，可通过启用状态控制调度");
        if(key.equals("consumption"))fail(count("SELECT COUNT(*) FROM mm_task_context WHERE operation_id=?",get(key,id).get("operation_id"))>0,"工序已下发，不能删除耗料定义，请建立新版本");
        if(key.equals("checks"))fail(count("SELECT COUNT(*) FROM mm_check_due WHERE plan_id=?",id)>0,"已有周期任务的点检计划不可删除，请停用");
        if(key.equals("defects"))fail(count("SELECT COUNT(*) FROM mm_nc WHERE defect_id=?",id)>0,"缺陷分类已用于质量处置，不能删除");
        if(key.equals("qc-lines"))fail(count("SELECT COUNT(*) FROM mm_document_check WHERE template_id=?",get(key,id).get("template_id"))>0,"模板已有批次检验记录，不能删除检测项");
        fail(key.equals("maintenance-plans")&&count("SELECT COUNT(*) FROM mm_maintenance_occurrence WHERE plan_id=?",id)>0,"已有保养任务的计划不可删除，请停用");
        Resource resource=definition(key);Map<String,Object> existing=get(key,id);
        fail(key.equals("check-records"),"点检记录不可删除");
        fail(key.equals("locations"),"库位保留历史引用，请勿删除");
        for(Resource other:catalog.values())for(Field field:other.fields())if(key.equals(field.ref()))fail(count("SELECT COUNT(*) FROM "+other.table()+" WHERE "+field.name()+"=?",id)>0,"记录已被"+other.label()+"引用，不能删除");
        if(key.equals("users"))throw new IllegalArgumentException("不支持删除账号");
        if(key.equals("materials"))for(String t:List.of("t_inventory","t_bom","t_stock_in_item","t_stock_out_item"))fail(count("SELECT COUNT(*) FROM "+t+" WHERE material_id=?",id)>0,"物料已有业务记录");
        if(key.equals("units"))fail(count("SELECT COUNT(*) FROM t_material WHERE unit=?",existing.get("uom_name"))+count("SELECT COUNT(*) FROM t_product WHERE unit=?",existing.get("uom_name"))>0,"单位已使用");
        if(key.equals("products"))fail(count("SELECT COUNT(*) FROM t_production_plan WHERE product_id=?",id)>0,"产品已有生产计划");
        if(key.equals("suppliers"))fail(count("SELECT COUNT(*) FROM t_purchase_order WHERE supplier_id=?",id)>0,"供应商已有采购单据");
        if(key.equals("customers"))fail(count("SELECT COUNT(*) FROM t_order WHERE customer_name=?",existing.get("customer_name"))>0,"客户已有销售订单");
        if(key.equals("equipment"))fail(count("SELECT COUNT(*) FROM t_maintenance WHERE equipment_id=?",id)>0,"设备已有维修记录");
        if(key.equals("warehouses"))for(String t:List.of("t_inventory","t_stock_in","t_stock_out","t_location"))fail(count("SELECT COUNT(*) FROM "+t+" WHERE warehouse_id=?",id)>0,"仓库已有库存或单据");
        if(key.equals("bom-versions"))fail(count("SELECT COUNT(*) FROM mm_work_order WHERE bom_id=?",id)>0,"BOM已关联工单");
        if(key.equals("routes"))fail(count("SELECT COUNT(*) FROM mm_work_order WHERE route_id=?",id)>0,"路线已关联工单");
        if(key.equals("operations"))fail(count("SELECT COUNT(*) FROM mm_task_context WHERE operation_id=?",id)>0,"工序已下发");
        if(key.equals("qc-templates"))fail(count("SELECT COUNT(*) FROM mm_inspection WHERE template_id=?",id)>0,"检测模板已有检验单");
        if(key.equals("schedules"))fail(count("SELECT COUNT(*) FROM mm_task_context WHERE schedule_id=?",id)>0,"排班已用于工单");
        if(key.equals("bom-lines"))fail(count("SELECT COUNT(*) FROM mm_work_order WHERE bom_id=?",existing.get("bom_id"))>0,"BOM已冻结");
        if(key.equals("qc-lines"))fail(count("SELECT COUNT(*) FROM mm_inspection WHERE template_id=?",existing.get("template_id"))>0,"检测模板已使用");
        db.update("DELETE FROM "+resource.table()+" WHERE id=?",id);audit(actor,key,"DELETE",id);
    }
    public List<Map<String,Object>> expand(long bomId,BigDecimal amount) {
        fail(amount.signum()<=0,"生产数量必须为正数");Map<String,Object> bom=get("bom-versions",bomId);
        List<Map<String,Object>> lines=db.queryForList("SELECT * FROM mm_bom_line WHERE bom_id=?",bomId);checkCycles(lines);
        Set<Long> connected=new HashSet<>();Deque<Long> nodes=new ArrayDeque<>();nodes.add(number(bom.get("root_material_id")));while(!nodes.isEmpty()){long node=nodes.removeFirst();if(!connected.add(node))continue;for(var l:lines)if(number(l.get("parent_material_id"))==node)nodes.add(number(l.get("material_id")));}
        Set<Long> reached=new HashSet<>();Map<Long,BigDecimal> leaves=new LinkedHashMap<>();
        expandNode(number(bom.get("root_material_id")),amount,lines,leaves,reached);
        for(var l:lines)fail(!connected.contains(number(l.get("parent_material_id"))),"BOM存在未连接到根节点的组成");
        List<Map<String,Object>> result=new ArrayList<>();
        for(var leaf:leaves.entrySet()){Map<String,Object> material=new LinkedHashMap<>(get("materials",leaf.getKey()));material.put("required_quantity",leaf.getValue());result.add(material);}
        return result;
    }
    private void expandNode(long parent,BigDecimal amount,List<Map<String,Object>> lines,Map<Long,BigDecimal> leaves,Set<Long> reached) {
        boolean root=reached.isEmpty();reached.add(parent);List<Map<String,Object>> children=lines.stream().filter(l->number(l.get("parent_material_id"))==parent).toList();var material=get("materials",parent);String sourcing=text(material.get("sourcing_mode"));
        if(children.isEmpty()||!root&&sourcing.equals("BUY")){fail(number(material.get("material_type"))!=1&&!Set.of("BUY","BOTH").contains(sourcing),"自制半成品或成品缺少子BOM；外购物资请设置供应方式");leaves.merge(parent,amount,BigDecimal::add);return;}
        for(var child:children)expandNode(number(child.get("material_id")),amount.multiply(decimal(child.get("quantity"))),lines,leaves,reached);
    }
    public List<Map<String,Object>> workOrders() {return db.queryForList("SELECT w.*,p.plan_no,p.product_id,p.product_name,p.completed_quantity FROM mm_work_order w JOIN t_production_plan p ON p.id=w.plan_id ORDER BY w.id DESC");}
    public List<Map<String,Object>> workTasks(long id) {return db.queryForList("SELECT t.*,c.operation_id,c.station_id,c.schedule_id,c.sequence_no,c.is_terminal,o.name AS operation_name FROM mm_task_context c JOIN t_production_task t ON t.id=c.task_id JOIN mm_operation o ON o.id=c.operation_id WHERE c.work_order_id=? ORDER BY c.sequence_no",id);}
    public List<Map<String,Object>> plans() {return db.queryForList("SELECT p.id,CONCAT(p.plan_no,' · ',COALESCE(p.product_name,''),' · 计划量',p.plan_quantity) AS name,p.product_id,p.plan_quantity,p.plan_status FROM t_production_plan p WHERE p.plan_status<2 AND NOT EXISTS(SELECT 1 FROM mm_work_order w WHERE w.plan_id=p.id) ORDER BY p.id DESC");}
    @Transactional
    public Map<String,Object> createWorkOrder(Map<String,Object> v,long actor) throws Exception {
        fulfillment.lock();
        long planId=number(v.get("plan_id"));Map<String,Object> plan=db.queryForMap("SELECT * FROM t_production_plan WHERE id=? FOR UPDATE",planId);
        fail(count("SELECT COUNT(*) FROM mm_work_order WHERE plan_id=?",planId)>0,"此计划已有制造工单");
        fail(count("SELECT COUNT(*) FROM t_production_task WHERE plan_id=?",planId)>0,"计划已有旧任务，请另建计划，不能重复下发");
        Map<String,Object> bom=get("bom-versions",number(v.get("bom_id"))),route=get("routes",number(v.get("route_id")));
        fail(number(bom.get("product_id"))!=number(plan.get("product_id"))||number(route.get("product_id"))!=number(plan.get("product_id")),"产品、BOM与路线不一致");
        List<Map<String,Object>> flat=expand(number(bom.get("id")),BigDecimal.ONE);
        List<Map<String,Object>> operations=db.queryForList("SELECT * FROM mm_operation WHERE route_id=? ORDER BY sequence_no",route.get("id"));fail(operations.isEmpty(),"路线没有工序");
        fail(number(plan.get("plan_quantity"))<=0 || number(plan.get("plan_status"))>=2,"计划数量或状态不允许下发");
        long owner=number(v.get("owner_id"));get("users",owner);long scheduleId=number(v.get("schedule_id"));
        if(scheduleId>0){Map<String,Object> schedule=get("schedules",scheduleId);fail(count("SELECT COUNT(*) FROM mm_member WHERE team_id=? AND user_id=?",schedule.get("team_id"),owner)==0,"负责人不属于排班班组");}
        Map<String,Object> data=new LinkedHashMap<>();data.put("code",mesRules.nextCode("WORKORDER_CODE"));data.put("plan_id",planId);data.put("bom_id",bom.get("id"));data.put("route_id",route.get("id"));data.put("quantity",plan.get("plan_quantity"));data.put("bom_snapshot",json.writeValueAsString(db.queryForList("SELECT * FROM mm_bom_line WHERE bom_id=?",bom.get("id"))));data.put("route_snapshot",json.writeValueAsString(operations));data.put("created_by",actor);
        fail(operations.stream().filter(op->number(op.get("is_key"))==1).count()!=1,"路线必须配置且只能配置一个关键工序");
        long wo=insert("mm_work_order",data);
        fulfillment.freeze(wo,flat,decimal(plan.get("plan_quantity")),number(bom.get("root_material_id")));
        lineStock.snapshot(wo,operations,number(bom.get("root_material_id")),decimal(plan.get("plan_quantity")));
        for(int i=0;i<operations.size();i++){
            var op=operations.get(i);Map<String,Object> user=get("users",owner);
            long task=insert("t_production_task",new LinkedHashMap<>(Map.of("task_no","MO-"+wo+"-"+op.get("sequence_no"),"plan_id",planId,"task_type",1,"product_id",plan.get("product_id"),"product_name",plan.get("product_name"),"quantity",plan.get("plan_quantity"),"assigned_user_id",owner,"assigned_user_name",user.get("name"),"task_status",1)));
            Map<String,Object> context=new LinkedHashMap<>();context.put("task_id",task);context.put("work_order_id",wo);context.put("operation_id",op.get("id"));context.put("sequence_no",op.get("sequence_no"));context.put("predecessor_id",op.get("predecessor_id"));context.put("requires_qc",op.get("requires_qc"));context.put("is_terminal",i==operations.size()-1?1:0);context.put("station_id",op.get("station_id"));context.put("schedule_id",scheduleId>0 && number(get("schedules",scheduleId).get("station_id"))==number(op.get("station_id"))?scheduleId:null);insert("mm_task_context",context);
        }
        // Compatibility: publish this product's current leaf BOM for existing read-only consumers.
        db.update("DELETE FROM t_bom WHERE product_id=?",plan.get("product_id"));
        for(var material:flat)db.update("INSERT INTO t_bom(product_id,material_id,material_name,quantity,unit,remark) VALUES(?,?,?,?,?,?)",plan.get("product_id"),material.get("id"),material.get("material_name"),material.get("required_quantity"),material.get("unit"),"BOM版本 "+bom.get("version"));
        db.update("UPDATE t_production_plan SET plan_status=1 WHERE id=?",planId);
        if(plan.get("order_id")!=null) db.update("UPDATE t_order SET order_status=2 WHERE id=? AND order_status<3",plan.get("order_id"));audit(actor,"work-orders","CREATE",wo);
        return workOrders().stream().filter(w->number(w.get("id"))==wo).findFirst().orElseThrow();
    }
    public void checkTaskPredecessors(long taskId) {
        mesRules.taskGate(taskId);
        fulfillment.requireIssued(taskId);
        var contexts=db.queryForList("SELECT * FROM mm_task_context WHERE task_id=?",taskId);if(contexts.isEmpty())return;
        var c=contexts.get(0);
        List<Map<String,Object>> predecessors=db.queryForList("SELECT t.task_status FROM mm_task_context x JOIN t_production_task t ON t.id=x.task_id WHERE x.work_order_id=? AND (x.operation_id=? OR ((? IS NOT NULL OR ?=1) AND x.sequence_no<?))",c.get("work_order_id"),c.get("predecessor_id"),c.get("predecessor_id"),c.get("is_terminal"),c.get("sequence_no"));
        for(var p:predecessors)fail(number(p.get("task_status"))!=4,"前置工序尚未完成检验放行");
    }
    public void lockTask(long id) { fulfillment.lock();db.queryForList("SELECT id FROM t_production_task WHERE id=? FOR UPDATE",id); }
    public void protectTask(long id) { fail(count("SELECT COUNT(*) FROM mm_task_context WHERE task_id=?",id)>0,"工单工序任务请通过开工、报工和检验操作推进，不可直接修改或删除"); }
    public void protectPlan(long id) { fail(count("SELECT COUNT(*) FROM mm_work_order WHERE plan_id=?",id)>0,"此计划已下发工单，不能追加独立任务"); }
    public void syncProgress(Long planId) {
        automation.capture(planId);
        db.update("UPDATE t_production_task t JOIN mm_task_context c ON c.task_id=t.id SET t.task_status=4 WHERE t.plan_id=? AND t.task_status=3 AND c.requires_qc=0",planId);
        if(planId==null)return;
        var work=db.queryForList("SELECT id FROM mm_work_order WHERE plan_id=?",planId);
        if(work.isEmpty()) {
            int finished=count("SELECT COALESCE(SUM(quantity),0) FROM t_production_task WHERE plan_id=? AND task_status=4",planId);
            db.update("UPDATE t_production_plan SET completed_quantity=?,plan_status=1 WHERE id=?",finished,planId);return;
        }
        long wo=number(work.get(0).get("id"));
        int completed=count("SELECT COALESCE(SUM(t.quantity),0) FROM mm_task_context c JOIN t_production_task t ON t.id=c.task_id JOIN mm_operation o ON o.id=c.operation_id WHERE c.work_order_id=? AND o.is_key=1 AND t.task_status=4",wo);
        int pending=count("SELECT COUNT(*) FROM mm_task_context c JOIN t_production_task t ON t.id=c.task_id WHERE c.work_order_id=? AND t.task_status<>4",wo);
        db.update("UPDATE t_production_plan SET completed_quantity=?,plan_status=? WHERE id=?",completed,pending==0?2:1,planId);
        db.update("UPDATE mm_work_order SET status=? WHERE id=? AND status<>'SCRAP_HOLD'",pending==0?"PRODUCTION_COMPLETED":"IN_PROGRESS",wo);
        // Production completion does not imply delivery; shipping will advance the order in stage two.
    }
    public List<Map<String,Object>> inspections() { return db.queryForList("SELECT * FROM mm_inspection ORDER BY id DESC"); }
    @Transactional
    public Map<String,Object> inspect(Map<String,Object> v,long actor) throws Exception {
        fulfillment.lock();
        long templateId=number(v.get("template_id"));Map<String,Object> template=get("qc-templates",templateId);
        String kind=text(template.get("kind")),sourceType=text(v.get("source_type"));long sourceId=number(v.get("source_id"));
        String sourceTable=switch(kind){case "PROCESS"->"t_production_task";case "INCOMING"->"t_purchase_order";case "SHIPPING"->"t_order";default->throw new IllegalArgumentException("检验类型无效");};
        String expected=switch(kind){case "PROCESS"->"TASK";case "INCOMING"->"PURCHASE";default->"ORDER";};
        fail(!expected.equals(sourceType),"来源类型与检验模板不匹配");
        var sources=db.queryForList("SELECT * FROM "+sourceTable+" WHERE id=? FOR UPDATE",sourceId);fail(sources.isEmpty(),"来源单据不存在");var source=sources.get(0);
        int quantity=Math.toIntExact(number(v.get("quantity"))),qualified=Math.toIntExact(number(v.get("qualified_quantity")));
        fail(quantity<=0||qualified<0||qualified>quantity,"检验数量或合格数量无效");
        if(kind.equals("SHIPPING")) {
            fail(number(source.get("deleted"))!=0 || number(source.get("order_status"))!=2,"只可登记生产中的有效销售订单出货检验");
            fail(!text(source.get("product_name")).equals(text(get("products",number(template.get("product_id"))).get("product_name"))),"出货模板产品不匹配");
            fail(quantity!=number(source.get("quantity")),"出货检验数量须等于订单数量");
            fail(count("SELECT COUNT(*) FROM t_production_plan WHERE order_id=? AND plan_status=2 AND completed_quantity>=plan_quantity",sourceId)==0,"来源订单生产计划尚未完成");
            fail(count("SELECT COUNT(*) FROM mm_stock_posting s JOIN mm_work_order w ON w.id=s.source_id JOIN t_production_plan p ON p.id=w.plan_id WHERE s.kind='RECEIPT' AND p.order_id=?",sourceId)==0,"成品须先入库，再执行出货检验");
        }
        if(kind.equals("INCOMING")) {
            fail(number(source.get("is_deleted"))!=0 || !"APPROVED".equals(text(source.get("status"))),"请先审批有效且未入库的采购订单再登记来料检验");
            fail(count("SELECT COALESCE(SUM(quantity),0) FROM t_purchase_order_item WHERE po_id=?",sourceId)!=quantity,"来料检验数量须等于采购明细整批总数量");
            fail(count("SELECT COUNT(*) FROM t_purchase_order_item i WHERE i.po_id=? AND NOT EXISTS(SELECT 1 FROM mm_bom_line l JOIN mm_bom b ON b.id=l.bom_id WHERE l.material_id=i.material_id AND b.product_id=?)",sourceId,template.get("product_id"))>0,"采购物料与检测模板产品的BOM不匹配");
        }
        if(!kind.equals("PROCESS")) fail(count("SELECT COUNT(*) FROM mm_inspection WHERE source_type=? AND source_id=? AND status='RELEASED'",sourceType,sourceId)>0,"此来源已检验放行，请勿重复登记");
        boolean releasePhase=Set.of("FINAL","LAST").contains(text(template.get("phase")));
        if(kind.equals("PROCESS")){fail(releasePhase?number(source.get("task_status"))!=3:!Set.of(1L,2L,3L).contains(number(source.get("task_status"))),"当前任务状态不允许执行此阶段检验");fail(releasePhase?quantity!=number(source.get("quantity")):quantity>number(source.get("quantity")),"检验数量与任务/抽检范围不符");fail(number(source.get("product_id"))!=number(template.get("product_id")),"模板产品不匹配");
            fail(number(template.get("operation_id"))==0||count("SELECT COUNT(*) FROM mm_task_context WHERE task_id=? AND operation_id=?",sourceId,template.get("operation_id"))!=1,"检测模板不适用于该工序");
            fail(count("SELECT COUNT(*) FROM mm_nc WHERE task_id=? AND status<>'CLOSED'",sourceId)>0,"请先完成该任务的不合格处置或返工，再进行复检");
        }
        List<Map<String,Object>> required=db.queryForList("SELECT i.* FROM mm_qc_line l JOIN mm_qc_item i ON i.id=l.item_id WHERE l.template_id=?",templateId);fail(required.isEmpty(),"模板没有检测项");
        Map<String,Object> results=json.convertValue(v.get("results"),new TypeReference<Map<String,Object>>(){});fail(results==null,"请填写实测结果");boolean passed=qualified==quantity;
        List<Map<String,Object>> snapshot=new ArrayList<>();
        for(var item:required){Object raw=results.get(text(item.get("id")));fail(!(raw instanceof Map),"请完成所有检测项");Map<?,?> result=(Map<?,?>)raw;fail(text(result.get("actual")).isEmpty(),"实测值不能为空");String verdict=text(result.get("result"));fail(!Set.of("PASS","FAIL").contains(verdict),"检测项结果无效");
            if("NUMBER".equals(item.get("data_type"))){BigDecimal actual;try{actual=decimal(result.get("actual"));}catch(RuntimeException e){throw new IllegalArgumentException("数值检测项必须填写有效数字");}verdict=actual.compareTo(decimal(item.get("lower_limit")))>=0&&actual.compareTo(decimal(item.get("upper_limit")))<=0?"PASS":"FAIL";}
            passed&=verdict.equals("PASS");snapshot.add(Map.of("item",item,"actual",result.get("actual"),"result",verdict));}
        if(kind.equals("PROCESS")&&releasePhase){
            long declaredBad=Objects.requireNonNull(db.queryForObject("SELECT COALESCE(SUM(bad_quantity),0) FROM mm_report_sheet WHERE task_id=? AND status='CONFIRMED'",Long.class,sourceId));
            boolean reworked=count("SELECT COUNT(*) FROM mm_rework r JOIN mm_nc n ON n.id=r.nc_id WHERE n.task_id=? AND r.status='DONE'",sourceId)>0;
            if(declaredBad>0&&!reworked){passed=false;qualified=Math.min(qualified,Math.max(0,quantity-Math.toIntExact(declaredBad)));}
        }
        if(!passed&&qualified==quantity)qualified=0; // A failed measured item cannot be recorded as an entirely qualified batch.
        Map<String,Object> data=new LinkedHashMap<>();data.put("code",mesRules.nextCode(kind.equals("INCOMING")?"QC_IQC_CODE":kind.equals("PROCESS")?"IPQC_CODE":"OQC_CODE"));data.put("template_id",templateId);data.put("kind",kind);data.put("source_type",sourceType);data.put("source_id",sourceId);data.put("quantity",quantity);data.put("qualified_quantity",qualified);data.put("results",json.writeValueAsString(snapshot));data.put("status",passed?"RELEASED":"BLOCKED");data.put("actor_id",actor);long id=insert("mm_inspection",data);
        if(kind.equals("PROCESS")&&(releasePhase||!passed))automation.inspected(sourceId,passed,id);
        if(!passed)db.update("INSERT INTO mm_nc(inspection_id,task_id,quantity) VALUES(?,?,?)",id,kind.equals("PROCESS")?sourceId:null,qualified==quantity?quantity:quantity-qualified);
        if(kind.equals("PROCESS")&&passed&&releasePhase){db.update("UPDATE t_production_task SET task_status=4 WHERE id=?",sourceId);syncProgress(number(source.get("plan_id")));}
        audit(actor,"inspections","EXECUTE",id);return db.queryForMap("SELECT * FROM mm_inspection WHERE id=?",id);
    }
}
