package com.mouse.mes.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SkillAgentService {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final IntelligenceService intelligence;
    private final AgentModelGateway model;
    private final AutomationService automation;
    private final TransactionTemplate tx;
    private final List<Map<String,Object>> registry;
    public SkillAgentService(JdbcTemplate db,ObjectMapper json,IntelligenceService intelligence,AgentModelGateway model,AutomationService automation,PlatformTransactionManager manager) throws java.io.IOException {
        this.db=db;this.json=json;this.intelligence=intelligence;this.model=model;this.automation=automation;tx=new TransactionTemplate(manager);
        try(var stream=new ClassPathResource("agent/skills.json").getInputStream()){registry=json.readValue(stream,new TypeReference<>(){});}
    }
    public List<Map<String,Object>> skills(int role){return registry.stream().filter(s->((List<?>)s.get("roles")).stream().anyMatch(r->((Number)r).intValue()==role)).toList();}
    private Map<String,Object> skill(String id,int role){return skills(role).stream().filter(s->id.equals(s.get("id"))).findFirst().orElseThrow(()->new SecurityException("当前岗位无权使用此技能，或技能不存在"));}
    public Map<String,Object> catalog(int role){return Map.of("skills",skills(role),"model",model.config(),"canConfirm",role==6||role==9);}
    static String localRoute(String q){
        if(q.matches("(?s).*(补货|采购建议).*"))return "replenishment";
        if(q.matches("(?s).*(点检|保养|维护待办).*"))return "maintenance";
        if(q.matches("(?s).*(追溯|工单|订单编号|计划单).*"))return "trace";
        if(q.matches("(?s).*(缺料|齐套|安全库存|库存不足|低于.*库存|原料.*不够).*"))return "shortage";
        if(q.matches("(?s).*(库存|出入库|原材料|半成品|成品|盘点|结存|余额).*"))return "inventory";
        return "";
    }
    public Map<String,Object> message(Map<String,Object> body,long actor,int role) throws Exception {
        String q=Objects.toString(body.get("question"),"").trim();
        if(q.isBlank()||q.length()>1200)throw new IllegalArgumentException("请填写1至1200字的任务");
        String selected=Objects.toString(body.get("skillId"),"");
        String id=selected;
        if(id.isBlank()){
            if(model.remote()){
                try{model.reserve(actor);id=model.route(q,skills(role));}
                catch(Exception e){save(actor,"help",q,"FAILED",Map.of(),Map.of("summary","模型技能识别失败，本次未执行业务任务。"));throw e;}
            }
            else {
                id=localRoute(q);
                if(id.isBlank()&&q.matches("(?s)^(改查|只看|物料|仓库|产品|清除|全部).*")){
                    String previous=Objects.toString(body.get("previousSkill"),"");
                    if(Set.of("inventory","shortage","trace").contains(previous))id=previous;
                }
            }
        }
        if(id.isBlank())return save(actor,"help",q,"CLARIFICATION",Map.of(),Map.of("summary","请一次描述一个任务，或选择下方技能。可查询库存、缺料、工单追溯、点检保养待办，也可处理补货建议。暂不支持任意业务操作。"));
        var definition=skill(id,role);
        Map<String,Object> reply=new LinkedHashMap<>();reply.put("skillName",definition.get("name"));reply.put("mode",model.config().get("mode"));
        try {
            String status="SUCCEEDED";
            switch(id){
                case "inventory","shortage","trace" -> {
                    String prefix=switch(id){case "inventory"->"库存查询：";case "shortage"->"缺料查询：";default->"追溯查询：";};
                    Map<String,Object> query=new LinkedHashMap<>();query.put("question",prefix+q);
                    if(body.containsKey("sessionId")&&id.equals(body.get("previousSkill")))query.put("sessionId",body.get("sessionId"));
                    var result=intelligence.query(query,actor,role);
                    reply.put("query",result);reply.put("sessionId",result.get("sessionId"));
                    if(result.containsKey("result")){
                        Map<?,?> data=(Map<?,?>)result.get("result");reply.put("data",data);
                        int count=data.get("rows") instanceof List<?> rows?rows.size():0;
                        reply.put("summary","已查询真实业务数据，返回 "+count+" 条主记录。数量按物料、仓库和单位分别展示，请结合来源与查询条件复核。");
                    }else{status="CLARIFICATION";reply.put("summary","还需要补充条件，请按下面的提示继续描述。");}
                }
                case "maintenance" -> {
                    var rows=db.queryForList("SELECT * FROM (SELECT d.id,d.due_date,d.status,p.code,p.name,p.plan_type,e.equipment_name,u.real_name owner_name FROM mm_check_due d JOIN mm_check_plan p ON p.id=d.plan_id JOIN t_equipment e ON e.id=p.equipment_id LEFT JOIN t_user u ON u.id=p.owner_id WHERE d.status IN ('PENDING','OVERDUE') AND d.due_date<=CURDATE() UNION ALL SELECT o.id,o.planned_at due_date,m.status,m.maintenance_no code,p.name,'周期保养' plan_type,e.equipment_name,u.real_name owner_name FROM mm_maintenance_occurrence o JOIN mm_maintenance_plan p ON p.id=o.plan_id JOIN t_maintenance m ON m.id=o.maintenance_id JOIN t_equipment e ON e.id=p.equipment_id LEFT JOIN t_user u ON u.id=p.owner_id WHERE m.status IN ('PENDING','PROCESSING') AND o.planned_at<=NOW()) tasks ORDER BY due_date,id LIMIT 201");
                    reply.put("data",Map.of("rows",rows.stream().limit(200).toList(),"updatedAt",LocalDateTime.now().toString(),"evidence","来源：周期点检保养计划及已生成的到期待办；截至今天未完成，最多显示200条。"));
                    reply.put("summary",rows.isEmpty()?"当前没有已生成且到期未完成的点检保养任务；这不代表所有设备都已配置周期计划。":"找到 "+Math.min(rows.size(),200)+" 条到期未完成的点检保养任务"+(rows.size()>200?"（结果已截断）":"")+"，请进入 MES 运营中心或设备管理的周期保养页面处理。");
                }
                case "replenishment" -> {
                    var rows=automation.suggestions();reply.put("canPropose",role==6||role==9);
                    reply.put("data",Map.of("rows",rows.stream().limit(200).toList(),"updatedAt",LocalDateTime.now().toString(),"evidence","来源：实时库存与物料安全库存。建议数量仅按安全库存计算，与缺料技能的未领料需求口径不同。最多显示200条。"));
                    reply.put("summary","找到 "+rows.size()+" 种低于安全库存的物料。选择具体物料可生成确认任务；确认只登记补货建议，不会创建采购订单或改变库存。");
                }
                default -> throw new IllegalArgumentException("尚未实现此技能");
            }
            return save(actor,id,q,status,Map.of(),reply);
        }catch(Exception e){save(actor,id,q,"FAILED",Map.of(),Map.of("summary","任务执行失败，未返回业务结论；请检查条件或服务配置。"));throw e;}
    }
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("任务数据序列化失败");}}
    private Map<String,Object> decode(Object value){try{return json.readValue(value.toString(),new TypeReference<>(){});}catch(Exception e){throw new IllegalArgumentException("任务记录格式异常");}}
    private Map<String,Object> save(long actor,String skill,String question,String status,Map<String,Object> payload,Map<String,Object> result){
        String id=UUID.randomUUID().toString();
        db.update("INSERT INTO mm_agent_run(id,user_id,skill_id,question,status,payload,result) VALUES(?,?,?,?,?,?,?)",id,actor,skill,question,status,encode(payload),encode(result));
        return detail(id,actor);
    }
    public List<Map<String,Object>> history(long actor){return db.queryForList("SELECT id,skill_id,question,status,created_at,updated_at FROM mm_agent_run WHERE user_id=? ORDER BY created_at DESC LIMIT 30",actor);}
    public List<Map<String,Object>> history(long actor,int role){var allowed=new HashSet<String>();allowed.add("help");skills(role).forEach(s->allowed.add(s.get("id").toString()));return history(actor).stream().filter(r->allowed.contains(r.get("skill_id"))).toList();}
    public Map<String,Object> detailForRole(String id,long actor,int role){var run=detail(id,actor);if(!"help".equals(run.get("skill_id")))skill(run.get("skill_id").toString(),role);return run;}
    public Map<String,Object> detail(String id,long actor){
        var rows=db.queryForList("SELECT id,skill_id,question,status,result,created_at,updated_at FROM mm_agent_run WHERE id=? AND user_id=?",id,actor);
        if(rows.isEmpty())throw new SecurityException("任务不存在或不属于当前用户");
        var result=new LinkedHashMap<String,Object>(rows.get(0));result.put("reply",decode(result.remove("result")));return result;
    }
    private Map<String,Object> suggestion(long material){return automation.suggestions().stream().filter(r->((Number)r.get("material_id")).longValue()==material).findFirst().orElseThrow(()->new IllegalArgumentException("此物料当前不需要安全库存补货，请刷新建议"));}
    private void confirmer(int role){if(role!=6&&role!=9)throw new SecurityException("只有采购负责人或管理员可以确认补货建议");}
    public Map<String,Object> propose(long material,long actor,int role){
        confirmer(role);var current=suggestion(material);
        if("CONFIRMED".equals(current.get("status")))throw new IllegalArgumentException("该物料补货建议已确认，请在业务页面复核，勿重复提交");
        return save(actor,"replenishment","确认补货建议："+current.get("material_name"),"PENDING",current,Map.of("summary","请核对物料、当前库存和建议数量。确认后只登记补货建议，采购订单仍需单独创建。任务10分钟内有效。","preview",current));
    }
    public Map<String,Object> confirm(String id,long actor,int role){
        confirmer(role);
        return tx.execute(s->{
            var rows=db.queryForList("SELECT * FROM mm_agent_run WHERE id=? AND user_id=? FOR UPDATE",id,actor);
            if(rows.isEmpty())throw new SecurityException("任务不存在或不属于当前用户");
            var run=rows.get(0);
            if(!"replenishment".equals(run.get("skill_id")))throw new IllegalArgumentException("此技能不可确认写入");
            if("SUCCEEDED".equals(run.get("status")))return detail(id,actor);
            if(!"PENDING".equals(run.get("status")))throw new IllegalArgumentException("只有待确认任务可以执行");
            LocalDateTime created=run.get("created_at") instanceof LocalDateTime t?t:((java.sql.Timestamp)run.get("created_at")).toLocalDateTime();
            if(created.plusMinutes(10).isBefore(LocalDateTime.now()))return invalidate(id,actor,"EXPIRED","确认任务已过期，请重新生成建议。");
            var original=decode(run.get("payload"));long material=((Number)original.get("material_id")).longValue();
            // Same lock as the existing confirmation service serializes proposals for one material.
            db.queryForList("SELECT id FROM t_material WHERE id=? FOR UPDATE",material);
            db.queryForList("SELECT id FROM t_inventory WHERE material_id=? AND is_deleted=0 FOR UPDATE",material);
            Map<String,Object> current;
            try{current=suggestion(material);}catch(IllegalArgumentException e){return invalidate(id,actor,"STALE","物料已无需补货，请刷新建议。");}
            if("CONFIRMED".equals(current.get("status")))return invalidate(id,actor,"STALE","该物料已确认补货，请刷新建议。");
            for(String field:List.of("suggested_quantity","current_quantity"))if(new BigDecimal(original.get(field).toString()).compareTo(new BigDecimal(current.get(field).toString()))!=0)return invalidate(id,actor,"STALE","库存或建议数量已经变化，请刷新后重新核对。");
            automation.confirm(material,actor);
            db.update("INSERT INTO mm_audit(actor_id,resource,action,record_id) VALUES(?,'agent:replenishment','CONFIRM',?)",actor,material);
            db.update("UPDATE mm_agent_run SET status='SUCCEEDED',result=?,updated_at=NOW(6) WHERE id=?",encode(Map.of("summary","已确认补货建议，物料："+current.get("material_name")+"。未创建采购订单，库存未变更。","preview",current)),id);
            return detail(id,actor);
        });
    }
    private Map<String,Object> invalidate(String id,long actor,String status,String summary){db.update("UPDATE mm_agent_run SET status=?,result=?,updated_at=NOW(6) WHERE id=?",status,encode(Map.of("summary",summary)),id);return detail(id,actor);}
    public Map<String,Object> cancel(String id,long actor){
        db.update("UPDATE mm_agent_run SET status='CANCELLED',updated_at=NOW(6) WHERE id=? AND user_id=? AND status='PENDING'",id,actor);return detail(id,actor);
    }
}
