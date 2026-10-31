package com.mouse.mes.mapper;

import com.mouse.mes.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    List<User> findAll();
    User findById(@Param("id") Long id);
    User findByUsername(@Param("username") String username);
    User findByUserCode(@Param("userCode") String userCode);
    int insert(User user);
    int update(User user);
    int delete(@Param("id") Long id);
    List<User> findByRoleId(@Param("roleId") Long roleId);
    List<User> findByRoleCode(@Param("roleCode") String roleCode);
    List<User> findByDeptId(@Param("deptId") Long deptId);
    List<User> findByDeptCode(@Param("deptCode") String deptCode);
    List<User> findAllIncludingDeleted();
    List<User> findDeletedUsers();
    int softDeleteById(@Param("id") Long id, @Param("leaveDate") java.util.Date leaveDate);
    int restoreUser(@Param("id") Long id);
    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);
    int updateProfile(@Param("id") Long id, @Param("realName") String realName,
                      @Param("phone") String phone, @Param("email") String email,
                      @Param("gender") Integer gender);
}
