package com.mouse.mes.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
@Service
public class RoleAccessService {
 private final JdbcTemplate db;
 public RoleAccessService(JdbcTemplate db){this.db=db;}
 public long effective(long role){
  var roles=db.queryForList("SELECT status FROM t_role WHERE id=?",role);
  if(roles.isEmpty()||((Number)roles.get(0).get("status")).intValue()!=1)throw new SecurityException("岗位已停用，请联系管理员");
  if(role<=11)return role;
  var rows=db.queryForList("SELECT base_role_id FROM mm_role_profile WHERE role_id=?",role);
  if(rows.isEmpty())throw new SecurityException("岗位尚未配置业务权限");
  long base=((Number)rows.get(0).get("base_role_id")).longValue();
  if(base<1||base>11||base==9)throw new SecurityException("岗位权限配置无效");return base;
 }
}
