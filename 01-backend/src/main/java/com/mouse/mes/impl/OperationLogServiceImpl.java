package com.mouse.mes.impl;

import com.mouse.mes.entity.OperationLog;
import com.mouse.mes.mapper.OperationLogMapper;
import com.mouse.mes.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Override
    public List<OperationLog> findAll() {
        return operationLogMapper.findAll();
    }

    @Override
    public OperationLog findById(Long id) {
        return operationLogMapper.findById(id);
    }

    @Override
    public List<OperationLog> findByUserId(Long userId) {
        return operationLogMapper.findByUserId(userId);
    }

    @Override
    public OperationLog create(OperationLog operationLog) {
        operationLogMapper.insert(operationLog);
        return operationLog;
    }

    @Override
    public List<OperationLog> findByModule(String module) {
        List<OperationLog> all = operationLogMapper.findAll();
        return all;
    }

    @Override
    public List<OperationLog> findByTimeRange(String startTime, String endTime) {
        List<OperationLog> all = operationLogMapper.findAll();
        return all;
    }
}