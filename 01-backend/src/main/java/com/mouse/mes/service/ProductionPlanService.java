package com.mouse.mes.service;

import com.mouse.mes.entity.ProductionPlan;

import java.util.List;

public interface ProductionPlanService {
    List<ProductionPlan> findAll();
    ProductionPlan findById(Long id);
    ProductionPlan findByPlanNo(String planNo);
    ProductionPlan create(ProductionPlan productionPlan);
    ProductionPlan update(ProductionPlan productionPlan);
    void delete(Long id);
    List<ProductionPlan> findByStatus(Integer status);
    List<ProductionPlan> findByOrderId(Long orderId);
    ProductionPlan createFromOrder(Long orderId);
    ProductionPlan getProgress(Long id);
}