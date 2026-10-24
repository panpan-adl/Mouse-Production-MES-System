package com.mouse.mes.controller;

import com.mouse.mes.entity.OperationLog;
import com.mouse.mes.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operation-logs")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    public List<OperationLog> getAllOperationLogs() {
        return operationLogService.findAll();
    }

    @GetMapping("/{id}")
    public OperationLog getOperationLogById(@PathVariable Long id) {
        return operationLogService.findById(id);
    }

    @GetMapping("/user/{userId}")
    public List<OperationLog> getOperationLogsByUserId(@PathVariable Long userId) {
        return operationLogService.findByUserId(userId);
    }

    @GetMapping("/module/{module}")
    public List<OperationLog> getOperationLogsByModule(@PathVariable String module) {
        return operationLogService.findByModule(module);
    }

    @GetMapping("/time-range")
    public List<OperationLog> getOperationLogsByTimeRange(@RequestParam String startTime, @RequestParam String endTime) {
        return operationLogService.findByTimeRange(startTime, endTime);
    }
}