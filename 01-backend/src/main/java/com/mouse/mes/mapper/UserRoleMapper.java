package com.mouse.mes.mapper;

import com.mouse.mes.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserRoleMapper {
    List<UserRole> findByUserId(@Param("userId") Long userId);
    int insert(UserRole userRole);
    int deleteByUserId(@Param("userId") Long userId);
    int deleteByRoleCode(@Param("roleCode") String roleCode);
}
