package com.mouse.mes.service;

import com.mouse.mes.entity.OperationLog;

import java.util.List;

public interface OperationLogService {
    List<OperationLog> findAll();
    OperationLog findById(Long id);
    List<OperationLog> findByUserId(Long userId);
    OperationLog create(OperationLog operationLog);
    List<OperationLog> findByModule(String module);
    List<OperationLog> findByTimeRange(String startTime, String endTime);
}