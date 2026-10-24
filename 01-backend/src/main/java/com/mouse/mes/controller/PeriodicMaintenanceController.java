package com.mouse.mes.controller;
import com.mouse.mes.service.PeriodicMaintenanceService;
import com.mouse.mes.util.JsonResult;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/maintenances/periodic")
public class PeriodicMaintenanceController {
 private final PeriodicMaintenanceService service;
 public PeriodicMaintenanceController(PeriodicMaintenanceService service){this.service=service;}
 @GetMapping("/plans") public Object plans(){return JsonResult.success(service.plans());}
 @GetMapping("/tasks") public Object tasks(){return JsonResult.success(service.tasks());}
 @PostMapping("/generate") public Object generate(){service.generate();return JsonResult.success();}
}
