package com.mouse.mes.service;

import com.mouse.mes.entity.InventoryIn;

import java.util.List;

public interface InventoryInService {
    List<InventoryIn> findAll();
    InventoryIn findById(Long id);
    InventoryIn findByInNo(String inNo);
    InventoryIn create(InventoryIn inventoryIn);
    InventoryIn update(InventoryIn inventoryIn);
}