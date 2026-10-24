package com.mouse.mes.service;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** One eligibility boundary for selectors, persistence and scheduled execution. */
@Service
public class AssignableStaffService {
    private final JdbcTemplate db;
    public AssignableStaffService(JdbcTemplate db){this.db=db;}
    public List<Map<String,Object>> list(int role){
        if(!Set.of(3,4,8).contains(role))throw new IllegalArgumentException("不支持的任务负责岗位");
        return db.queryForList("SELECT u.id,u.user_code userCode,u.real_name name,u.real_name realName,u.real_name real_name,u.role_id assignedRoleId,COALESCE(p.base_role_id,r.id) roleId,u.status,u.deleted FROM t_user u JOIN t_role r ON r.id=u.role_id LEFT JOIN mm_role_profile p ON p.role_id=r.id WHERE u.deleted=0 AND u.status=1 AND r.status=1 AND COALESCE(p.base_role_id,r.id)=? ORDER BY u.real_name,u.id",role);
    }
    public Map<String,Object> require(long user,int role){
        return list(role).stream().filter(r->((Number)r.get("id")).longValue()==user).findFirst().orElseThrow(()->new IllegalArgumentException(role==8?"负责人必须是在职且岗位启用的设备维护员，请重新选择":role==4?"负责人必须是在职且岗位启用的质检员，请重新选择":"负责人必须是在职且岗位启用的操作工，请重新选择"));
    }
}
