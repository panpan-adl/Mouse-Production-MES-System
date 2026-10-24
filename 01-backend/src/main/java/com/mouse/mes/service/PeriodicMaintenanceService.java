package com.mouse.mes.service;

import com.mouse.mes.entity.Maintenance;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PeriodicMaintenanceService {
    @org.springframework.beans.factory.annotation.Autowired private AssignableStaffService assignableStaff;
    private final JdbcTemplate db; private final MaintenanceService maintenance; private final TransactionTemplate tx;
    public PeriodicMaintenanceService(JdbcTemplate db,MaintenanceService maintenance,TransactionTemplate tx){this.db=db;this.maintenance=maintenance;this.tx=tx;}
    public static LocalDateTime dateTime(Object value){if(value instanceof LocalDateTime date)return date;if(value instanceof java.sql.Timestamp date)return date.toLocalDateTime();return LocalDateTime.parse(value.toString().replace(' ','T'));}
    static LocalDateTime asTime(Object value){return value instanceof LocalDateTime t?t:((java.sql.Timestamp)value).toLocalDateTime();}
    public static LocalDateTime occurrence(LocalDateTime anchor,long index,long frequency,String unit){
        long amount=Math.multiplyExact(index,frequency);
        return switch(unit){case "DAY"->anchor.plusDays(amount);case "WEEK"->anchor.plusWeeks(amount);case "MONTH"->anchor.plusMonths(amount);case "YEAR"->anchor.plusYears(amount);default->throw new IllegalArgumentException("周期单位无效");};
    }
    public List<Map<String,Object>> plans(){
        var rows=db.queryForList("SELECT p.*,e.equipment_name,u.real_name owner_name FROM mm_maintenance_plan p JOIN t_equipment e ON e.id=p.equipment_id JOIN t_user u ON u.id=p.owner_id ORDER BY p.id DESC");
        for(var p:rows){var anchor=dateTime(p.get("anchor_at"));p.put("next_at",occurrence(anchor,((Number)p.get("next_index")).longValue(),((Number)p.get("frequency")).longValue(),p.get("cycle_unit").toString()));}
        return rows;
    }
    public List<Map<String,Object>> tasks(){return db.queryForList("SELECT o.*,p.name plan_name,m.status,m.start_time actual_start,m.end_time actual_end FROM mm_maintenance_occurrence o JOIN mm_maintenance_plan p ON p.id=o.plan_id JOIN t_maintenance m ON m.id=o.maintenance_id ORDER BY o.planned_at DESC");}
    @Scheduled(initialDelay=60000,fixedDelay=60000)
    public void generate(){
        for(var row:db.queryForList("SELECT id FROM mm_maintenance_plan WHERE enabled=1")){
            long id=((Number)row.get("id")).longValue();
            try{tx.executeWithoutResult(status->{
                var p=db.queryForMap("SELECT * FROM mm_maintenance_plan WHERE id=? FOR UPDATE",id);if(((Number)p.get("enabled")).intValue()!=1)return;
                long index=((Number)p.get("next_index")).longValue();var anchor=dateTime(p.get("anchor_at"));
                LocalDateTime due=occurrence(anchor,index,((Number)p.get("frequency")).longValue(),p.get("cycle_unit").toString());
                if(due.minusDays(((Number)p.get("remind_days")).longValue()).isAfter(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))))return;
                var person=assignableStaff.require(((Number)p.get("owner_id")).longValue(),8);
                Maintenance m=new Maintenance();m.setMaintenanceNo("PM-"+id+"-"+index);m.setEquipmentId(((Number)p.get("equipment_id")).longValue());m.setMaintenanceType(1);
                m.setMaintenanceUserId(((Number)p.get("owner_id")).longValue());m.setMaintenanceUserName(person.get("real_name").toString());m.setMaintenanceContent(p.get("content").toString());m.setFaultDesc("周期保养");m.setCost(java.math.BigDecimal.ZERO);
                m.setRemark("计划："+p.get("name")+"；应执行："+due+"；到期不自动完成");
                Maintenance created=maintenance.create(m);
                db.update("INSERT INTO mm_maintenance_occurrence(plan_id,occurrence_index,maintenance_id,planned_at) VALUES(?,?,?,?)",id,index,created.getId(),due);
                db.update("UPDATE mm_maintenance_plan SET next_index=next_index+1,last_error=NULL WHERE id=?",id);
            });}catch(Exception error){db.update("UPDATE mm_maintenance_plan SET last_error=? WHERE id=?",Objects.toString(error.getMessage(),"任务生成失败").substring(0,Math.min(500,Objects.toString(error.getMessage(),"任务生成失败").length())),id);}
        }
    }
}
