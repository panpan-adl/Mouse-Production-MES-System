package com.mouse.mes.service;

import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.entity.User;
import com.mouse.mes.mapper.*;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Self-service account changes use the authenticated ID and explicit field lists. */
@Service
public class AccountService implements CommandLineRunner {
    private final JdbcTemplate db;
    private final UserMapper users;
    private final RoleMapper roles;
    private final DepartmentMapper departments;
    private final RoleAccessService access;
    public AccountService(JdbcTemplate db, UserMapper users, RoleMapper roles, DepartmentMapper departments, RoleAccessService access) {
        this.db=db; this.users=users; this.roles=roles; this.departments=departments; this.access=access;
    }
    @Override public void run(String... args) {
        db.execute("CREATE TABLE IF NOT EXISTS mm_account_settings(user_id BIGINT PRIMARY KEY,login_protection TINYINT NOT NULL DEFAULT 0,session_version BIGINT NOT NULL DEFAULT 0,updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        db.execute("CREATE TABLE IF NOT EXISTS mm_account_alert(id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT NOT NULL,content VARCHAR(500) NOT NULL,is_read TINYINT NOT NULL DEFAULT 0,created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,INDEX idx_account_alert(user_id,id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }
    public User requireUser(long id) {
        User user=users.findById(id);
        if(user==null)throw new IllegalArgumentException("账号不存在");
        return user;
    }
    public LoginResponse profile(long id) {
        User user=requireUser(id);
        LoginResponse result=new LoginResponse();
        result.setId(user.getId()); result.setUserCode(user.getUserCode()); result.setUsername(user.getUsername());
        result.setRealName(user.getRealName()); result.setPhone(user.getPhone()); result.setEmail(user.getEmail());
        result.setGender(user.getGender()); result.setAvatar(user.getAvatar()); result.setStatus(user.getStatus());
        result.setRoleId(user.getRoleId()); result.setPermissionRoleId(access.effective(user.getRoleId()));
        var role=roles.findById(user.getRoleId().intValue()); result.setRoleName(role==null?"":role.getRoleName());
        result.setDeptId(user.getDeptId());
        var department=user.getDeptId()==null?null:departments.findById(user.getDeptId());
        result.setDeptName(department==null?"":department.getDeptName());
        return result;
    }
    @Transactional public LoginResponse updateProfile(long id, String name, String phone, String email, Integer gender) {
        if(name==null||name.isBlank()||name.trim().length()>50)throw new IllegalArgumentException("真实姓名须为1至50个字符");
        if(phone==null||(!phone.isBlank()&&!phone.matches("^1[3-9]\\d{9}$")))throw new IllegalArgumentException("请输入有效的11位手机号码，或留空");
        if(email==null||email.length()>100||(!email.isBlank()&&!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")))throw new IllegalArgumentException("请输入有效的邮箱地址，或留空");
        if(gender==null||gender<0||gender>2)throw new IllegalArgumentException("性别值无效");
        requireUser(id);
        db.update("UPDATE t_user SET real_name=?,phone=?,email=?,gender=?,update_time=NOW() WHERE id=? AND deleted=0",name.trim(),phone.trim(),email.trim(),gender,id);
        return profile(id);
    }
    public long sessionVersion(long id) {
        var rows=db.queryForList("SELECT session_version FROM mm_account_settings WHERE user_id=?",id);
        return rows.isEmpty()?0L:((Number)rows.get(0).get("session_version")).longValue();
    }
    public boolean validSession(LoginResponse identity) {
        return sessionVersion(identity.getId())==(identity.getSessionVersion()==null?0:identity.getSessionVersion());
    }
    @Transactional public void changePassword(long id, String oldPassword, String newPassword, String confirmPassword) {
        User user=requireUser(id);
        if(oldPassword==null||!oldPassword.equals(user.getPassword()))throw new IllegalArgumentException("旧密码不正确");
        if(newPassword==null||newPassword.length()<6||newPassword.length()>64||newPassword.isBlank())throw new IllegalArgumentException("新密码须为6至64个字符");
        if(!newPassword.equals(confirmPassword))throw new IllegalArgumentException("两次输入的新密码不一致");
        if(newPassword.equals(oldPassword))throw new IllegalArgumentException("新密码不能与旧密码相同");
        int changed=db.update("UPDATE t_user SET password=?,update_time=NOW() WHERE id=? AND password=? AND deleted=0",newPassword,id,oldPassword);
        if(changed!=1)throw new IllegalArgumentException("密码已变更，请重新登录后再试");
        db.update("INSERT INTO mm_account_settings(user_id,session_version) VALUES(?,1) ON DUPLICATE KEY UPDATE session_version=session_version+1",id);
    }
    public Map<String,Object> security(long id) {
        User user=requireUser(id);
        var rows=db.queryForList("SELECT login_protection FROM mm_account_settings WHERE user_id=?",id);
        return Map.of("loginProtection",!rows.isEmpty()&&((Number)rows.get(0).get("login_protection")).intValue()==1,"passwordSet",user.getPassword()!=null&&!user.getPassword().isBlank());
    }
    public void setProtection(long id, boolean enabled) {
        db.update("INSERT INTO mm_account_settings(user_id,login_protection) VALUES(?,?) ON DUPLICATE KEY UPDATE login_protection=VALUES(login_protection)",id,enabled?1:0);
    }
    public List<Map<String,Object>> loginHistory(long id) {
        return db.queryForList("SELECT id,login_time AS loginTime,login_ip AS loginIp,browser,status,msg FROM t_login_log WHERE user_id=? ORDER BY login_time DESC,id DESC LIMIT 50",id);
    }
    public List<Map<String,Object>> alerts(long id) {
        return db.queryForList("SELECT id,'账号安全提醒' AS title,content,'warning' AS type,is_read AS isRead,created_at AS createdAt FROM mm_account_alert WHERE user_id=? ORDER BY id DESC LIMIT 50",id);
    }
    public void readAlert(long id, long alert) { db.update("UPDATE mm_account_alert SET is_read=1 WHERE id=? AND user_id=?",alert,id); }
    public void recordLogin(Long id, String ip, int status) {
        if(id==null)return;
        var settings=db.queryForList("SELECT login_protection FROM mm_account_settings WHERE user_id=?",id);
        if(settings.isEmpty()||((Number)settings.get(0).get("login_protection")).intValue()!=1)return;
        User user=users.findById(id);
        boolean newAddress=status==1&&user!=null&&user.getLastLoginIp()!=null&&!user.getLastLoginIp().isBlank()&&!user.getLastLoginIp().equals(ip);
        if(status==0||newAddress)db.update("INSERT INTO mm_account_alert(user_id,content) VALUES(?,?)",id,(status==0?"检测到登录失败":"检测到新的登录地址")+"，来源 IP："+ip+"。如非本人操作，请及时修改密码。");
    }
}
