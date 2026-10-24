package com.mouse.mes.controller;
import com.mouse.mes.service.RoleService;
import com.mouse.mes.entity.Role;
import com.mouse.mes.util.JsonResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/roles/manage")
public class RoleAccessController {
 private final RoleService roles;private final JdbcTemplate db;
 public RoleAccessController(RoleService roles,JdbcTemplate db){this.roles=roles;this.db=db;}
 @GetMapping public Object list(){return JsonResult.success(db.queryForList("SELECT r.*,COALESCE(p.base_role_id,r.id) base_role_id,(SELECT COUNT(*) FROM t_user u WHERE u.role_id=r.id AND u.deleted=0) member_count FROM t_role r LEFT JOIN mm_role_profile p ON p.role_id=r.id ORDER BY r.sort,r.id"));}
 @GetMapping("/{id}/members") public Object members(@PathVariable long id){return JsonResult.success(db.queryForList("SELECT real_name,status FROM t_user WHERE role_id=? AND deleted=0",id));}
 @PostMapping @Transactional public Object save(@RequestBody Map<String,Object> v){
  Long id=v.get("id")==null?null:Long.valueOf(v.get("id").toString());long base=Long.parseLong(v.get("base_role_id").toString());
  if(base<1||base>11||base==9&&(id==null||id!=9))throw new IllegalArgumentException("请选择普通业务岗位权限；管理员不可复制");
  String name=Objects.toString(v.get("role_name"),"").trim();if(name.isEmpty()||name.length()>50)throw new IllegalArgumentException("岗位名称须为1至50字");
  Role role=new Role();role.setRoleName(name);role.setRoleDesc(Objects.toString(v.get("role_desc"),""));role.setStatus(Integer.valueOf(v.get("status").toString()));
  if(role.getStatus()!=0&&role.getStatus()!=1)throw new IllegalArgumentException("状态无效");
  if(id==null){role=roles.create(role);id=role.getId().longValue();}else{role.setId(id.intValue());roles.update(role);}
  if(id>11)db.update("INSERT INTO mm_role_profile(role_id,base_role_id) VALUES(?,?) ON DUPLICATE KEY UPDATE base_role_id=VALUES(base_role_id)",id,base);
  return JsonResult.success(id);
 }
 @ExceptionHandler({IllegalArgumentException.class,SecurityException.class}) @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST) public Object invalid(Exception e){return JsonResult.error(e.getMessage());}
}
