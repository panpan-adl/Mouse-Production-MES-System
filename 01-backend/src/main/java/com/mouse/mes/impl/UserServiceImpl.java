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
    public User create(User user) {
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
