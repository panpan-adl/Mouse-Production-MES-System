package com.mouse.mes.impl;

import com.mouse.mes.entity.ProductionOrder;
import com.mouse.mes.mapper.ProductionOrderMapper;
import com.mouse.mes.service.ProductionOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ProductionOrderServiceImpl implements ProductionOrderService {

    @Autowired
    private ProductionOrderMapper productionOrderMapper;

    @Override
    public List<ProductionOrder> findAll() {
        return productionOrderMapper.findAll();
    }

    @Override
    public ProductionOrder findById(Long id) {
        return productionOrderMapper.findById(id);
    }

    @Override
    public ProductionOrder findByPoNo(String poNo) {
        return productionOrderMapper.findByPoNo(poNo);
    }

    @Override
    public ProductionOrder create(ProductionOrder productionOrder) {
        productionOrder.setCreatedAt(new Date());
        productionOrder.setUpdatedAt(new Date());
        productionOrder.setIsDeleted(0);
        if (productionOrder.getStatus() == null) {
            productionOrder.setStatus("PLANNED");
        }
        productionOrderMapper.insert(productionOrder);
        return productionOrder;
    }

    @Override
    public ProductionOrder update(ProductionOrder productionOrder) {
        ProductionOrder existing = productionOrderMapper.findById(productionOrder.getId());
        if (existing == null) {
            throw new RuntimeException("生产订单不存在");
        }
        if (productionOrder.getPoNo() != null) {
            existing.setPoNo(productionOrder.getPoNo());
        }
        if (productionOrder.getProductId() != null) {
            existing.setProductId(productionOrder.getProductId());
        }
        if (productionOrder.getPlannedQuantity() != null) {
            existing.setPlannedQuantity(productionOrder.getPlannedQuantity());
        }
        if (productionOrder.getActualQuantity() != null) {
            existing.setActualQuantity(productionOrder.getActualQuantity());
        }
        if (productionOrder.getPlannedStartDate() != null) {
            existing.setPlannedStartDate(productionOrder.getPlannedStartDate());
        }
        if (productionOrder.getPlannedEndDate() != null) {
            existing.setPlannedEndDate(productionOrder.getPlannedEndDate());
        }
        if (productionOrder.getActualStartDate() != null) {
            existing.setActualStartDate(productionOrder.getActualStartDate());
        }
        if (productionOrder.getActualEndDate() != null) {
            existing.setActualEndDate(productionOrder.getActualEndDate());
        }
        if (productionOrder.getStatus() != null) {
            existing.setStatus(productionOrder.getStatus());
        }
        if (productionOrder.getDepartmentId() != null) {
            existing.setDepartmentId(productionOrder.getDepartmentId());
        }
        existing.setUpdatedAt(new Date());
        productionOrderMapper.update(existing);
        return productionOrderMapper.findById(productionOrder.getId());
    }

    @Override
    public void delete(Long id) {
        ProductionOrder productionOrder = productionOrderMapper.findById(id);
        if (productionOrder != null) {
            productionOrder.setIsDeleted(1);
            productionOrder.setUpdatedAt(new Date());
            productionOrderMapper.update(productionOrder);
        }
    }
}
