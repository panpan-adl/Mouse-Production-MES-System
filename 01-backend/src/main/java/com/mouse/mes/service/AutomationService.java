package com.mouse.mes.service;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/** Transactional outbox: only committed reports become visible to the worker. */
@Service
public class AutomationService {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    public AutomationService(JdbcTemplate db,PlatformTransactionManager manager){this.db=db;this.tx=new TransactionTemplate(manager);}
    public void capture(Long plan){
        db.update("INSERT IGNORE INTO mm_auto_event(rule_code,task_id,status,error) SELECT 'REPORT_QC',t.id,IF(r.enabled=1,'PENDING','SKIPPED'),IF(r.enabled=1,NULL,'规则已停用') FROM t_production_task t JOIN mm_task_context c ON c.task_id=t.id JOIN mm_auto_rule r ON r.code='REPORT_QC' WHERE t.plan_id=? AND t.task_status=3 AND c.requires_qc=1",plan);
    }
    public void inspected(long task,boolean passed,long inspection){
        db.update("UPDATE mm_qc_todo SET status=?,inspection_id=?,completed_at=IF(?=1,NOW(6),NULL) WHERE task_id=?",passed?"DONE":"BLOCKED",inspection,passed?1:0,task);
    }
    @Scheduled(fixedDelay=2000,initialDelay=10000)
    public void tick(){
        try { if(db.queryForObject("SELECT COUNT(*) FROM mm_job_config WHERE code='REPORT_QC' AND enabled=1 AND (last_run IS NULL OR TIMESTAMPDIFF(SECOND,last_run,NOW())>=interval_seconds)",Integer.class)==0)return;for(int n=0;n<20;n++)if(!process())break;db.update("UPDATE mm_job_config SET last_run=NOW() WHERE code='REPORT_QC'"); }
        catch(org.springframework.dao.DataAccessException ignored){ /* Startup before additive migration: next tick retries. */ }
    }
    public boolean process(){
        return Boolean.TRUE.equals(tx.execute(s->{
            var events=db.queryForList("SELECT * FROM mm_auto_event WHERE status='PENDING' AND attempts<3 ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED");
            if(events.isEmpty())return false;
            var e=events.get(0);long id=((Number)e.get("id")).longValue(),task=((Number)e.get("task_id")).longValue();
            try {
                var rows=db.queryForList("SELECT c.work_order_id,t.task_status,p.product_id FROM mm_task_context c JOIN t_production_task t ON t.id=c.task_id JOIN t_production_plan p ON p.id=t.plan_id WHERE t.id=? AND c.requires_qc=1",task);
                if(rows.isEmpty()||((Number)rows.get(0).get("task_status")).intValue()!=3){db.update("UPDATE mm_auto_event SET status='SKIPPED',error='任务已放行或不再需要质检',finished_at=NOW(6) WHERE id=?",id);return true;}
                var row=rows.get(0);
                Integer count=db.queryForObject("SELECT COUNT(*) FROM mm_qc_template q JOIN mm_task_context c ON c.operation_id=q.operation_id WHERE c.task_id=? AND q.kind='PROCESS' AND q.phase IN ('FINAL','LAST') AND q.product_id=? AND EXISTS(SELECT 1 FROM mm_qc_line i WHERE i.template_id=q.id)",Integer.class,task,row.get("product_id"));
                if(count==null||count==0)throw new IllegalArgumentException("产品未配置有效过程检验模板，请完善后重试");
                db.update("INSERT IGNORE INTO mm_qc_todo(event_id,task_id,work_order_id) VALUES(?,?,?)",id,task,row.get("work_order_id"));
                db.update("UPDATE mm_auto_event SET status='SUCCEEDED',attempts=attempts+1,error=NULL,finished_at=NOW(6) WHERE id=?",id);
            } catch(IllegalArgumentException|org.springframework.dao.DataAccessException ex){db.update("UPDATE mm_auto_event SET attempts=attempts+1,status='FAILED',error=?,finished_at=NOW(6) WHERE id=?",ex instanceof IllegalArgumentException?ex.getMessage():"数据依赖或写入失败，请检查配置后重试",id);}
            return true;
        }));
    }
    public Map<String,Object> state(){
        return Map.of("rules",db.queryForList("SELECT * FROM mm_auto_rule"),"events",db.queryForList("SELECT e.*,w.code work_order_code,t.task_no FROM mm_auto_event e JOIN mm_task_context c ON c.task_id=e.task_id JOIN mm_work_order w ON w.id=c.work_order_id JOIN t_production_task t ON t.id=e.task_id ORDER BY e.id DESC LIMIT 200"),"todos",db.queryForList("SELECT q.*,w.code work_order_code,t.task_no,TIMESTAMPDIFF(MICROSECOND,e.created_at,q.created_at)/1000000.0 creation_seconds FROM mm_qc_todo q LEFT JOIN mm_auto_event e ON e.id=q.event_id JOIN mm_work_order w ON w.id=q.work_order_id JOIN t_production_task t ON t.id=q.task_id ORDER BY q.id DESC LIMIT 200"));
    }
    public void toggle(boolean enabled){db.update("UPDATE mm_auto_rule SET enabled=? WHERE code='REPORT_QC'",enabled?1:0);}
    public void retry(long id){int changed=db.update("UPDATE mm_auto_event e JOIN mm_auto_rule r ON r.code=e.rule_code SET e.status='PENDING',e.error=NULL WHERE e.id=? AND e.status IN ('FAILED','SKIPPED') AND e.attempts<3 AND r.enabled=1",id);if(changed==0)throw new IllegalArgumentException("仅可重试已启用规则的失败/跳过事件，最多三次");}
    public List<Map<String,Object>> suggestions(){return db.queryForList("SELECT m.id material_id,m.material_code,m.material_name,m.unit,m.safety_stock,COALESCE(SUM(i.quantity),0) current_quantity,GREATEST(0,m.safety_stock-COALESCE(SUM(i.quantity),0)) suggested_quantity,COALESCE(MAX(r.status),'SUGGESTED') status FROM t_material m LEFT JOIN t_inventory i ON i.material_id=m.id AND i.is_deleted=0 LEFT JOIN mm_replenishment r ON r.material_id=m.id WHERE m.status=1 GROUP BY m.id HAVING current_quantity<m.safety_stock ORDER BY suggested_quantity DESC");}
    public void confirm(long material,long actor){tx.executeWithoutResult(s->{var rows=db.queryForList("SELECT id FROM t_material WHERE id=? FOR UPDATE",material);if(rows.isEmpty()||suggestions().stream().noneMatch(r->((Number)r.get("material_id")).longValue()==material))throw new IllegalArgumentException("物料不存在或当前无需补货");db.update("INSERT INTO mm_replenishment(material_id,status,confirmed_by,confirmed_at) VALUES(?,'CONFIRMED',?,NOW()) ON DUPLICATE KEY UPDATE status='CONFIRMED',confirmed_by=?,confirmed_at=NOW()",material,actor,actor);});}
}
