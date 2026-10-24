package com.mouse.mes.service;

import com.mouse.mes.entity.Role;

import java.util.List;

public interface RoleService {
    List<Role> findAll();
    Role findById(Integer id);
    Role findByRoleCode(String roleCode);
    Role create(Role role);
    Role update(Role role);
    void delete(Integer id);
}