package com.mouse.mes.impl;

import com.mouse.mes.entity.Role;
import com.mouse.mes.mapper.RoleMapper;
import com.mouse.mes.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;
    @Autowired
    private RoleMapper roleMapper;

    @Override
    public List<Role> findAll() {
        return roleMapper.findAll();
    }

    @Override
    public Role findById(Integer id) {
        return roleMapper.findById(id);
    }

    @Override
    public Role findByRoleCode(String roleCode) {
        return roleMapper.findByRoleCode(roleCode);
    }

    @Override
    public Role create(Role role) {
        if (role.getRoleCode() == null || role.getRoleCode().isEmpty()) {
            role.setRoleCode("ROLE-"+java.util.UUID.randomUUID().toString().substring(0,12));
        }
        if (role.getRoleName() == null || role.getRoleName().isEmpty()) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        if (role.getSort() == null) {
            role.setSort(0);
        }
        roleMapper.insert(role);
        return role;
    }

    @Override
    public Role update(Role role) {
        if(role.getId()<=11&&Integer.valueOf(0).equals(role.getStatus()))throw new IllegalArgumentException("内置岗位不能停用");
        if(role.getRoleName()!=null&&role.getRoleName().isBlank())throw new IllegalArgumentException("岗位名称不能为空");
        role.setUpdateTime(new Date());
        roleMapper.update(role);
        return roleMapper.findById(role.getId());
    }

    @Override
    public void delete(Integer id) {
        if(id<=11)throw new IllegalArgumentException("内置岗位不能删除");
        if(db.queryForObject("SELECT COUNT(*) FROM t_user WHERE role_id=? AND deleted=0",Long.class,id)>0)throw new IllegalArgumentException("岗位仍有关联员工，请先调整岗位");
        roleMapper.delete(id);
    }
}