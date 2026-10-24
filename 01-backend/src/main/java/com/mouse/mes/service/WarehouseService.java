package com.mouse.mes.service;

import com.mouse.mes.entity.Warehouse;

import java.util.List;

public interface WarehouseService {
    List<Warehouse> findAll();
    Warehouse findById(Long id);
    Warehouse findByWarehouseCode(String warehouseCode);
    Warehouse create(Warehouse warehouse);
    Warehouse update(Warehouse warehouse);
    void delete(Long id);
}