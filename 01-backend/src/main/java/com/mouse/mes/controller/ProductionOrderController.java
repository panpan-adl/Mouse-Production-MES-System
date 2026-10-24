package com.mouse.mes.controller;

import com.mouse.mes.entity.ProductionOrder;
import com.mouse.mes.service.ProductionOrderService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production-order")
public class ProductionOrderController {

    @Autowired
    private ProductionOrderService productionOrderService;

    @GetMapping("/list")
    public JsonResult<List<ProductionOrder>> findAll() {
        return JsonResult.success(productionOrderService.findAll());
    }

    @GetMapping("/{id}")
    public JsonResult<ProductionOrder> findById(@PathVariable Long id) {
        ProductionOrder productionOrder = productionOrderService.findById(id);
        if (productionOrder == null) {
            return JsonResult.error("生产订单不存在");
        }
        return JsonResult.success(productionOrder);
    }

    @GetMapping("/no/{poNo}")
    public JsonResult<ProductionOrder> findByPoNo(@PathVariable String poNo) {
        ProductionOrder productionOrder = productionOrderService.findByPoNo(poNo);
        if (productionOrder == null) {
            return JsonResult.error("生产订单不存在");
        }
        return JsonResult.success(productionOrder);
    }

    @PostMapping("/add")
    public JsonResult<ProductionOrder> create(@RequestBody ProductionOrder productionOrder) {
        if (productionOrder.getPoNo() == null || productionOrder.getPoNo().isEmpty()) {
            return JsonResult.error("订单编号不能为空");
        }
        if (productionOrder.getProductId() == null) {
            return JsonResult.error("产品ID不能为空");
        }
        return JsonResult.success(productionOrderService.create(productionOrder));
    }

    @PutMapping("/{id}")
    public JsonResult<ProductionOrder> update(@PathVariable Long id, @RequestBody ProductionOrder productionOrder) {
        ProductionOrder existing = productionOrderService.findById(id);
        if (existing == null) {
            return JsonResult.error("生产订单不存在");
        }
        productionOrder.setId(id);
        return JsonResult.success(productionOrderService.update(productionOrder));
    }

    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Long id) {
        ProductionOrder existing = productionOrderService.findById(id);
        if (existing == null) {
            return JsonResult.error("生产订单不存在");
        }
        productionOrderService.delete(id);
        return JsonResult.success();
    }
}
