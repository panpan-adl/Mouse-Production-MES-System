package com.mouse.mes.controller;

import com.mouse.mes.dto.ProductionTaskActionRequest;
import com.mouse.mes.dto.ProductionTaskReportRequest;
import com.mouse.mes.entity.ProductionTask;
import com.mouse.mes.service.ProductionTaskService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production-tasks")
public class ProductionTaskController {
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper sessionMapper;
    private com.mouse.mes.dto.LoginResponse identity(jakarta.servlet.http.HttpServletRequest request) {
        return sessionMapper.convertValue(request.getAttribute("userInfo"), com.mouse.mes.dto.LoginResponse.class);
    }

    @Autowired
    private ProductionTaskService productionTaskService;

    @GetMapping
    public JsonResult<List<ProductionTask>> getAllProductionTasks() {
        return JsonResult.success(productionTaskService.findAll());
    }

    @GetMapping("/{id}")
    public JsonResult<ProductionTask> getProductionTaskById(@PathVariable Long id) {
        ProductionTask task = productionTaskService.findById(id);
        if (task == null) {
            return JsonResult.error("生产任务不存在");
        }
        return JsonResult.success(task);
    }

    @GetMapping("/taskno/{taskNo}")
    public JsonResult<ProductionTask> getProductionTaskByTaskNo(@PathVariable String taskNo) {
        ProductionTask task = productionTaskService.findByTaskNo(taskNo);
        if (task == null) {
            return JsonResult.error("生产任务不存在");
        }
        return JsonResult.success(task);
    }

    @GetMapping("/plan/{planId}")
    public JsonResult<List<ProductionTask>> getProductionTasksByPlanId(@PathVariable Long planId) {
        try {
            return JsonResult.success(productionTaskService.findByPlanId(planId));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PostMapping
    public JsonResult<ProductionTask> createProductionTask(@RequestBody ProductionTask productionTask) {
        if (productionTask.getTaskNo() == null || productionTask.getTaskNo().isEmpty()) {
            return JsonResult.error("任务编号不能为空");
        }
        if (productionTask.getPlanId() == null) {
            return JsonResult.error("计划ID不能为空");
        }
        return JsonResult.success(productionTaskService.create(productionTask));
    }

    /**
     * 操作工领取一条待领取生产任务。
     */
    @PutMapping("/{id}/claim")
    public JsonResult<ProductionTask> claimProductionTask(@PathVariable Long id,
                                                           @RequestBody ProductionTaskActionRequest request, jakarta.servlet.http.HttpServletRequest http) {
        try {
            return JsonResult.success(productionTaskService.claim(id, identity(http).getId(), identity(http).getRealName()));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 操作工开始已领取的生产任务。
     */
    @PutMapping("/{id}/start")
    public JsonResult<ProductionTask> startProductionTask(@PathVariable Long id,
                                                           @RequestBody ProductionTaskActionRequest request, jakarta.servlet.http.HttpServletRequest http) {
        try {
            return JsonResult.success(productionTaskService.start(id, identity(http).getId()));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 操作工提交本次实际完成数量；全部数量报工后任务自动转入待质检。
     */
    @PostMapping("/{id}/report")
    public JsonResult<ProductionTask> reportProductionTask(@PathVariable Long id,
                                                            @RequestBody ProductionTaskReportRequest request, jakarta.servlet.http.HttpServletRequest http) {
        try {
            return JsonResult.success(productionTaskService.report(
                    id,
                    identity(http).getId(),
                    identity(http).getRealName(),
                    request.getReportQuantity(),
                    request.getRemark()));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public JsonResult<ProductionTask> updateProductionTask(@PathVariable Long id, @RequestBody ProductionTask productionTask) {
        ProductionTask existing = productionTaskService.findById(id);
        if (existing == null) {
            return JsonResult.error("生产任务不存在");
        }
        productionTask.setId(id);
        return JsonResult.success(productionTaskService.update(productionTask));
    }

    @DeleteMapping("/{id}")
    public JsonResult<Void> deleteProductionTask(@PathVariable Long id) {
        ProductionTask existing = productionTaskService.findById(id);
        if (existing == null) {
            return JsonResult.error("生产任务不存在");
        }
        productionTaskService.delete(id);
        return JsonResult.success();
    }
}
