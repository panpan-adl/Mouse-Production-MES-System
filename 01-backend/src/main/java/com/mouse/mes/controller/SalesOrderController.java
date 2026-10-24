package com.mouse.mes.controller;

import com.mouse.mes.entity.SalesOrder;
import com.mouse.mes.service.SalesOrderService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales-order")
public class SalesOrderController {

    @Autowired
    private SalesOrderService salesOrderService;

    @GetMapping("/list")
    public JsonResult<List<SalesOrder>> findAll() {
        return JsonResult.success(salesOrderService.findAll());
    }

    @GetMapping("/{id}")
    public JsonResult<SalesOrder> findById(@PathVariable Long id) {
        SalesOrder salesOrder = salesOrderService.findById(id);
        if (salesOrder == null) {
            return JsonResult.error("销售订单不存在");
        }
        return JsonResult.success(salesOrder);
    }

    @GetMapping("/no/{soNo}")
    public JsonResult<SalesOrder> findBySoNo(@PathVariable String soNo) {
        SalesOrder salesOrder = salesOrderService.findBySoNo(soNo);
        if (salesOrder == null) {
            return JsonResult.error("销售订单不存在");
        }
        return JsonResult.success(salesOrder);
    }

    @PostMapping("/add")
    public JsonResult<SalesOrder> create(@RequestBody SalesOrder salesOrder) {
        if (salesOrder.getSoNo() == null || salesOrder.getSoNo().isEmpty()) {
            return JsonResult.error("订单编号不能为空");
        }
        if (salesOrder.getCustomerId() == null) {
            return JsonResult.error("客户ID不能为空");
        }
        return JsonResult.success(salesOrderService.create(salesOrder));
    }

    @PutMapping("/{id}")
    public JsonResult<SalesOrder> update(@PathVariable Long id, @RequestBody SalesOrder salesOrder) {
        SalesOrder existing = salesOrderService.findById(id);
        if (existing == null) {
            return JsonResult.error("销售订单不存在");
        }
        salesOrder.setId(id);
        return JsonResult.success(salesOrderService.update(salesOrder));
    }

    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Long id) {
        SalesOrder existing = salesOrderService.findById(id);
        if (existing == null) {
            return JsonResult.error("销售订单不存在");
        }
        salesOrderService.delete(id);
        return JsonResult.success();
    }

    @PutMapping("/{id}/confirm")
    public JsonResult<SalesOrder> confirm(@PathVariable Long id) {
        SalesOrder existing = salesOrderService.findById(id);
        if (existing == null) {
            return JsonResult.error("销售订单不存在");
        }
        if (!"PENDING".equals(existing.getStatus())) {
            return JsonResult.error("只有待领取的订单才能确认");
        }
        existing.setStatus("CONFIRMED");
        existing.setUpdatedAt(new Date());
        return JsonResult.success(salesOrderService.update(existing));
    }

    @PutMapping("/{id}/procure")
    public JsonResult<SalesOrder> updateMaterialStatus(@PathVariable Long id, @RequestBody String materialStatus) {
        SalesOrder existing = salesOrderService.findById(id);
        if (existing == null) {
            return JsonResult.error("销售订单不存在");
        }
        existing.setMaterialStatus(materialStatus);
        existing.setUpdatedAt(new Date());
        return JsonResult.success(salesOrderService.update(existing));
    }

    @GetMapping("/statistics")
    public JsonResult<Map<String, Object>> getStatistics() {
        return JsonResult.success(salesOrderService.getStatistics());
    }
}
