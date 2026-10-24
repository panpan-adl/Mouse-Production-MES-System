package com.mouse.mes.controller;

import com.mouse.mes.entity.ProductionReport;
import com.mouse.mes.service.ProductionReportService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production-reports")
public class ProductionReportController {

    @Autowired
    private ProductionReportService productionReportService;

    @GetMapping
    public JsonResult<List<ProductionReport>> getAllProductionReports() {
        return JsonResult.success(productionReportService.findAll());
    }

    @GetMapping("/{id}")
    public JsonResult<ProductionReport> getProductionReportById(@PathVariable Long id) {
        ProductionReport report = productionReportService.findById(id);
        if (report == null) {
            return JsonResult.error("报工记录不存在");
        }
        return JsonResult.success(report);
    }

    @GetMapping("/task/{taskId}")
    public JsonResult<List<ProductionReport>> getProductionReportsByTaskId(@PathVariable Long taskId) {
        return JsonResult.success(productionReportService.findByTaskId(taskId));
    }

    @GetMapping("/task/{taskId}/summary")
    public JsonResult<ProductionReport> getTaskSummary(@PathVariable Long taskId) {
        ProductionReport summary = productionReportService.getTaskSummary(taskId);
        if (summary == null) {
            ProductionReport empty = new ProductionReport();
            empty.setTaskId(taskId);
            empty.setReportQuantity(0);
            empty.setQualifiedQuantity(0);
            empty.setDefectQuantity(0);
            return JsonResult.success(empty);
        }
        return JsonResult.success(summary);
    }

    @GetMapping("/user/{userId}")
    public JsonResult<List<ProductionReport>> getProductionReportsByUserId(@PathVariable Long userId) {
        return JsonResult.success(productionReportService.findByReportUserId(userId));
    }

    @PostMapping
    public JsonResult<ProductionReport> createProductionReport(@RequestBody ProductionReport productionReport, jakarta.servlet.http.HttpServletRequest http) {
        com.mouse.mes.dto.LoginResponse actor=new com.fasterxml.jackson.databind.ObjectMapper().convertValue(http.getAttribute("userInfo"),com.mouse.mes.dto.LoginResponse.class);
        productionReport.setReportUserId(actor.getId());productionReport.setReportUserName(actor.getRealName());
        try {
            if (productionReport.getTaskId() == null) {
                return JsonResult.error("任务ID不能为空");
            }
            if (productionReport.getReportUserId() == null) {
                return JsonResult.error("汇报人ID不能为空");
            }
            return JsonResult.success(productionReportService.create(productionReport));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public JsonResult<ProductionReport> updateProductionReport(@PathVariable Long id, @RequestBody ProductionReport productionReport) {
        try {
            return JsonResult.success(productionReportService.update(id, productionReport));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }
}