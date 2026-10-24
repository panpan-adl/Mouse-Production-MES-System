package com.mouse.mes.impl;

import com.mouse.mes.entity.User;
import com.mouse.mes.mapper.UserMapper;
import com.mouse.mes.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;
    private void validateRole(Long role){if(role==null||db.queryForObject("SELECT COUNT(*) FROM t_role WHERE id=? AND status=1",Long.class,role)!=1)throw new IllegalArgumentException("请选择启用的岗位");}

    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }

    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User findByUserCode(String userCode) {
        return userMapper.findByUserCode(userCode);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public User create(User user) {
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        validateRole(user.getRoleId());
        if(user.getUserCode()==null||user.getUserCode().isBlank()){
            long next=db.queryForObject("SELECT COALESCE(MAX(CAST(user_code AS UNSIGNED)),100000)+1 FROM t_user WHERE user_code REGEXP '^[0-9]{6}$'",Long.class);
            if(next>999999)throw new IllegalArgumentException("员工编号已用尽");user.setUserCode(String.format("%06d",next));
        }
        if(user.getRealName()==null||user.getRealName().isBlank()||user.getUsername()==null||user.getUsername().isBlank()||user.getPassword()==null||user.getPassword().length()<6)throw new IllegalArgumentException("请填写姓名、登录名和至少6位密码");
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setDeleted(0);
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);
        return user;
    }

    @Override
    public User update(User user) {
        if(user.getRoleId()!=null)validateRole(user.getRoleId());
        user.setUpdateTime(new Date());
        userMapper.update(user);
        return userMapper.findById(user.getId());
    }

    @Override
    public void delete(Long id) {
        userMapper.delete(id);
    }

    @Override
    public List<User> findByRoleId(Long roleId) {
        return userMapper.findByRoleId(roleId);
    }

    @Override
    public List<User> findByRoleCode(String roleCode) {
        return userMapper.findByRoleCode(roleCode);
    }

    @Override
    public List<User> findByDeptId(Long deptId) {
        return userMapper.findByDeptId(deptId);
    }

    @Override
    public List<User> findAllIncludingDeleted() {
        return userMapper.findAllIncludingDeleted();
    }

    @Override
    public List<User> findDeletedUsers() {
        return userMapper.findDeletedUsers();
    }

    @Override
    public void employeeLeave(Long id) {
        userMapper.softDeleteById(id, new Date());
    }

    @Override
    public User restoreUser(Long id) {
        userMapper.restoreUser(id);
        return userMapper.findById(id);
    }

    @Override
    public void updateAvatar(Long id, String avatarUrl) {
        userMapper.updateAvatar(id, avatarUrl);
    }
}
