package com.mouse.mes.service;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operator declarations become inventory movements only after production approval. */
@Service
public class MesReportService {
    private final JdbcTemplate db;private final ProductionTaskService tasks;private final MesRulesService rules;
    public MesReportService(JdbcTemplate db,ProductionTaskService tasks,MesRulesService rules){this.db=db;this.tasks=tasks;this.rules=rules;}
    private static long id(Object v){return MesRulesService.id(v);}
    private static void require(boolean ok,String message){MesRulesService.require(ok,message);}
    public List<Map<String,Object>> list(long actor,int role){return db.queryForList("SELECT s.*,t.task_no,t.assigned_user_name,r.real_name reporter_name,u.real_name approver_name FROM mm_report_sheet s JOIN t_production_task t ON t.id=s.task_id JOIN t_user r ON r.id=s.reporter_id LEFT JOIN t_user u ON u.id=s.approved_by"+(role==3?" WHERE s.reporter_id=?":"")+" ORDER BY s.id DESC",role==3?new Object[]{actor}:new Object[]{});}
    @Transactional public long create(Map<String,Object> body,long actor,int role){
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");long task=id(body.get("task_id"));var t=db.queryForMap("SELECT * FROM t_production_task WHERE id=? FOR UPDATE",task);
        long reporter=role==3?actor:id(body.get("reporter_id"));require(reporter>0&&reporter==id(t.get("assigned_user_id")),"报工人必须为该任务负责人");
        require(Set.of(1L,2L).contains(id(t.get("task_status"))),"只有已派发或生产中的任务可以起草报工单");
        long good=id(body.get("good_quantity")),bad=id(body.get("bad_quantity"));require(good>=0&&bad>=0&&good<=Integer.MAX_VALUE&&bad<=Integer.MAX_VALUE&&good+bad>0&&good+bad<=Integer.MAX_VALUE,"合格品与不良品数量须为非负整数且总量大于零");
        long pending=db.queryForObject("SELECT COALESCE(SUM(good_quantity+bad_quantity),0) FROM mm_report_sheet WHERE task_id=? AND status='PENDING'",Long.class,task);
        long reported=db.queryForObject("SELECT COALESCE(SUM(report_quantity),0) FROM t_production_report WHERE task_id=?",Long.class,task);require(good+bad+pending+reported<=id(t.get("quantity")),"累计报工与待审批数量超过任务计划量");
        LocalDateTime time=body.get("report_time")==null?LocalDateTime.now():LocalDateTime.parse(body.get("report_time").toString().replace(' ','T'));require(!time.isAfter(LocalDateTime.now().plusMinutes(5)),"报工时间不能晚于当前时间");
        var slots=db.queryForList("SELECT * FROM mm_task_slot WHERE task_id=?",task);require(!slots.isEmpty(),"请先完成任务排产");var slot=slots.get(0);require(!time.isBefore(LocalDateTime.parse(slot.get("start_at").toString().replace(' ','T')))&&time.isBefore(LocalDateTime.parse(slot.get("end_at").toString().replace(' ','T'))),"实际报工时间必须在任务排产时段内");
        String remark=Objects.toString(body.get("remark"),"");require(remark.length()<=500,"备注最多500字");String code=rules.nextCode("REPORT_CODE");
        db.update("INSERT INTO mm_report_sheet(code,task_id,reporter_id,good_quantity,bad_quantity,report_time,remark,created_by) VALUES(?,?,?,?,?,?,?,?)",code,task,reporter,good,bad,time,remark,actor);return db.queryForObject("SELECT id FROM mm_report_sheet WHERE code=?",Long.class,code);
    }
    @Transactional public void decide(long sheet,boolean approved,String reason,long actor){
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");var s=db.queryForMap("SELECT * FROM mm_report_sheet WHERE id=? FOR UPDATE",sheet);String target=approved?"CONFIRMED":"REJECTED";
        if(target.equals(s.get("status")))return;require("PENDING".equals(s.get("status")),"报工单已处理，不能改变审批结论");
        if(!approved){require(reason!=null&&!reason.isBlank()&&reason.length()<=500,"请填写500字以内的驳回原因");db.update("UPDATE mm_report_sheet SET status='REJECTED',approved_by=?,approval_note=? WHERE id=?",actor,reason,sheet);return;}
        long task=id(s.get("task_id")),reporter=id(s.get("reporter_id"));var person=db.queryForMap("SELECT real_name FROM t_user WHERE id=? AND deleted=0 AND status<>0",reporter);
        tasks.report(task,reporter,person.get("real_name").toString(),Math.toIntExact(id(s.get("good_quantity"))+id(s.get("bad_quantity"))),Objects.toString(s.get("remark"),""));
        long report=db.queryForObject("SELECT MAX(id) FROM t_production_report WHERE task_id=?",Long.class,task);
        db.update("UPDATE t_production_report SET report_time=? WHERE id=?",s.get("report_time"),report);
        db.update("UPDATE mm_report_sheet SET status='CONFIRMED',approved_by=?,report_id=?,approved_at=NOW() WHERE id=?",actor,report,sheet);
    }
}
