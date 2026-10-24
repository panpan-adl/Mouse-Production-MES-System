package com.mouse.mes.service;

import com.mouse.mes.entity.ProductionOrder;

import java.util.List;

public interface ProductionOrderService {
    List<ProductionOrder> findAll();
    ProductionOrder findById(Long id);
    ProductionOrder findByPoNo(String poNo);
    ProductionOrder create(ProductionOrder productionOrder);
    ProductionOrder update(ProductionOrder productionOrder);
    void delete(Long id);
}
