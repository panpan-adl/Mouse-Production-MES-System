package com.mouse.mes.impl;

import com.mouse.mes.entity.LoginLog;
import com.mouse.mes.mapper.LoginLogMapper;
import com.mouse.mes.service.LoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoginLogServiceImpl implements LoginLogService {

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Override
    public List<LoginLog> findAll() {
        return loginLogMapper.findAll();
    }

    @Override
    public LoginLog findById(Long id) {
        return loginLogMapper.findById(id);
    }

    @Override
    public List<LoginLog> findByUserId(Long userId) {
        return loginLogMapper.findByUserId(userId);
    }

    @Override
    public LoginLog create(LoginLog loginLog) {
        loginLogMapper.insert(loginLog);
        return loginLog;
    }

    @Override
    public List<LoginLog> findByTimeRange(String startTime, String endTime) {
        List<LoginLog> all = loginLogMapper.findAll();
        return all;
    }
}