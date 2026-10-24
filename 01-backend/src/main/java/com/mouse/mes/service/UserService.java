package com.mouse.mes.service;

import com.mouse.mes.entity.User;

import java.util.List;

public interface UserService {
    List<User> findAll();
    User findById(Long id);
    User findByUsername(String username);
    User findByUserCode(String userCode);
    User create(User user);
    User update(User user);
    void delete(Long id);
    List<User> findByRoleId(Long roleId);
    List<User> findByRoleCode(String roleCode);
    List<User> findByDeptId(Long deptId);
    List<User> findAllIncludingDeleted();
    List<User> findDeletedUsers();
    void employeeLeave(Long id);
    User restoreUser(Long id);
    void updateAvatar(Long id, String avatarUrl);
}
