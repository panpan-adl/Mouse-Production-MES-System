package com.mouse.mes.service;

import com.mouse.mes.entity.InventoryOut;

import java.util.List;

public interface InventoryOutService {
    List<InventoryOut> findAll();
    InventoryOut findById(Long id);
    InventoryOut findByOutNo(String outNo);
    InventoryOut create(InventoryOut inventoryOut);
    InventoryOut update(InventoryOut inventoryOut);
}