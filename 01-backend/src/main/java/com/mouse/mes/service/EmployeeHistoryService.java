package com.mouse.mes.service;
import com.mouse.mes.entity.User;
import com.mouse.mes.dto.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class EmployeeHistoryService {
 private final JdbcTemplate db; private final HttpServletRequest request; private final ObjectMapper mapper;
 public EmployeeHistoryService(JdbcTemplate db,HttpServletRequest request,ObjectMapper mapper){this.db=db;this.request=request;this.mapper=mapper;}
 @PostConstruct public void initialize(){db.execute("CREATE TABLE IF NOT EXISTS mm_employee_history (id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,actor_id BIGINT NULL,old_dept_id BIGINT NULL,new_dept_id BIGINT NULL,old_department VARCHAR(120),new_department VARCHAR(120),old_role VARCHAR(120),new_role VARCHAR(120),action VARCHAR(40) NOT NULL,created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),INDEX idx_employee_history(user_id,id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");}
 private String department(Long id){if(id==null)return "未分配";var rows=db.queryForList("SELECT dept_name FROM t_department WHERE id=?",id);return rows.isEmpty()?"未建档部门 #"+id:String.valueOf(rows.get(0).get("dept_name"));}
 private String role(Long id){if(id==null)return "未分配";var rows=db.queryForList("SELECT role_name FROM t_role WHERE id=?",id);return rows.isEmpty()?"岗位 #"+id:String.valueOf(rows.get(0).get("role_name"));}
 public void record(User before,User after){
  Object info=request.getAttribute("userInfo");Long actor=info==null?null:mapper.convertValue(info,LoginResponse.class).getId();
  boolean changed=!Objects.equals(before.getDeptId(),after.getDeptId())||!Objects.equals(before.getRoleId(),after.getRoleId());
  db.update("INSERT INTO mm_employee_history(user_id,actor_id,old_dept_id,new_dept_id,old_department,new_department,old_role,new_role,action) VALUES(?,?,?,?,?,?,?,?,?)",after.getId(),actor,before.getDeptId(),after.getDeptId(),department(before.getDeptId()),department(after.getDeptId()),role(before.getRoleId()),role(after.getRoleId()),changed?"部门岗位调整":"部门岗位确认");
 }
 public List<Map<String,Object>> history(long user){return db.queryForList("SELECT h.*,u.real_name actor_name,u.user_code actor_code FROM mm_employee_history h LEFT JOIN t_user u ON u.id=h.actor_id WHERE h.user_id=? ORDER BY h.id DESC",user);}
}
