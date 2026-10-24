package com.mouse.mes.controller;

import com.mouse.mes.entity.LoginLog;
import com.mouse.mes.service.LoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/login-logs")
public class LoginLogController {

    @Autowired
    private LoginLogService loginLogService;

    @GetMapping
    public List<LoginLog> getAllLoginLogs() {
        return loginLogService.findAll();
    }

    @GetMapping("/{id}")
    public LoginLog getLoginLogById(@PathVariable Long id) {
        return loginLogService.findById(id);
    }

    @GetMapping("/user/{userId}")
    public List<LoginLog> getLoginLogsByUserId(@PathVariable Long userId) {
        return loginLogService.findByUserId(userId);
    }

    @GetMapping("/time-range")
    public List<LoginLog> getLoginLogsByTimeRange(@RequestParam String startTime, @RequestParam String endTime) {
        return loginLogService.findByTimeRange(startTime, endTime);
    }
}