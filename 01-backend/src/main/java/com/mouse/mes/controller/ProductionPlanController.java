package com.mouse.mes.controller;

import com.mouse.mes.entity.ProductionPlan;
import com.mouse.mes.service.ProductionPlanService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production-plans")
public class ProductionPlanController {

    @Autowired
    private ProductionPlanService productionPlanService;

    @GetMapping
    public JsonResult<List<ProductionPlan>> getAllProductionPlans() {
        return JsonResult.success(productionPlanService.findAll());
    }

    @GetMapping("/{id}")
    public JsonResult<ProductionPlan> getProductionPlanById(@PathVariable Long id) {
        ProductionPlan plan = productionPlanService.findById(id);
        if (plan == null) {
            return JsonResult.error("生产计划不存在");
        }
        return JsonResult.success(plan);
    }

    @GetMapping("/planno/{planNo}")
    public JsonResult<ProductionPlan> getProductionPlanByPlanNo(@PathVariable String planNo) {
        ProductionPlan plan = productionPlanService.findByPlanNo(planNo);
        if (plan == null) {
            return JsonResult.error("生产计划不存在");
        }
        return JsonResult.success(plan);
    }

    @GetMapping("/status/{status}")
    public JsonResult<List<ProductionPlan>> getProductionPlansByStatus(@PathVariable Integer status) {
        return JsonResult.success(productionPlanService.findByStatus(status));
    }

    @GetMapping("/order/{orderId}")
    public JsonResult<List<ProductionPlan>> getProductionPlansByOrderId(@PathVariable Long orderId) {
        return JsonResult.success(productionPlanService.findByOrderId(orderId));
    }

    @GetMapping("/{id}/progress")
    public JsonResult<ProductionPlan> getProgress(@PathVariable Long id) {
        try {
            return JsonResult.success(productionPlanService.getProgress(id));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PostMapping("/from-order/{orderId}")
    public JsonResult<ProductionPlan> createFromOrder(@PathVariable Long orderId) {
        try {
            return JsonResult.success(productionPlanService.createFromOrder(orderId));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PostMapping
    public JsonResult<ProductionPlan> createProductionPlan(@RequestBody ProductionPlan productionPlan) {
        if (productionPlan.getPlanNo() == null || productionPlan.getPlanNo().isEmpty()) {
            return JsonResult.error("计划编号不能为空");
        }
        if (productionPlan.getPlanQuantity() == null || productionPlan.getPlanQuantity() <= 0) {
            return JsonResult.error("计划数量必须大于0");
        }
        try {
            return JsonResult.success(productionPlanService.create(productionPlan));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public JsonResult<ProductionPlan> updateProductionPlan(@PathVariable Long id, @RequestBody ProductionPlan productionPlan) {
        ProductionPlan existing = productionPlanService.findById(id);
        if (existing == null) {
            return JsonResult.error("生产计划不存在");
        }
        productionPlan.setId(id);
        return JsonResult.success(productionPlanService.update(productionPlan));
    }

    @DeleteMapping("/{id}")
    public JsonResult<Void> deleteProductionPlan(@PathVariable Long id) {
        ProductionPlan existing = productionPlanService.findById(id);
        if (existing == null) {
            return JsonResult.error("生产计划不存在");
        }
        productionPlanService.delete(id);
        return JsonResult.success();
    }
}