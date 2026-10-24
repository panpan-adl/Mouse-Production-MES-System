package com.mouse.mes.service;

import com.mouse.mes.entity.LoginLog;

import java.util.List;

public interface LoginLogService {
    List<LoginLog> findAll();
    LoginLog findById(Long id);
    List<LoginLog> findByUserId(Long userId);
    LoginLog create(LoginLog loginLog);
    List<LoginLog> findByTimeRange(String startTime, String endTime);
}