package com.mouse.mes.mapper;

import com.mouse.mes.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LoginLogMapper {
    List<LoginLog> findAll();
    LoginLog findById(@Param("id") Long id);
    List<LoginLog> findByUserId(@Param("userId") Long userId);
    int insert(LoginLog loginLog);
}