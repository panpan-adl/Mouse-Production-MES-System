package com.mouse.mes.mapper;

import com.mouse.mes.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper {
    List<Role> findAll();
    Role findById(@Param("id") Integer id);
    Role findByRoleCode(@Param("roleCode") String roleCode);
    int insert(Role role);
    int update(Role role);
    int delete(@Param("id") Integer id);
}