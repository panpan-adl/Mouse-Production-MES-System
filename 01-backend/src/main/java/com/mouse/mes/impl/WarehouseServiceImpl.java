package com.mouse.mes.impl;

import com.mouse.mes.entity.Warehouse;
import com.mouse.mes.mapper.WarehouseMapper;
import com.mouse.mes.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class WarehouseServiceImpl implements WarehouseService {

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Override
    public List<Warehouse> findAll() {
        return warehouseMapper.findAll();
    }

    @Override
    public Warehouse findById(Long id) {
        return warehouseMapper.findById(id);
    }

    @Override
    public Warehouse findByWarehouseCode(String warehouseCode) {
        return warehouseMapper.findByWarehouseCode(warehouseCode);
    }

    @Override
    public Warehouse create(Warehouse warehouse) {
        warehouse.setCreateTime(new Date());
        warehouse.setUpdateTime(new Date());
        if (warehouse.getStatus() == null) {
            warehouse.setStatus(1);
        }
        warehouseMapper.insert(warehouse);
        return warehouse;
    }

    @Override
    public Warehouse update(Warehouse warehouse) {
        warehouse.setUpdateTime(new Date());
        warehouseMapper.update(warehouse);
        return warehouseMapper.findById(warehouse.getId());
    }

    @Override
    public void delete(Long id) {
        warehouseMapper.delete(id);
    }
}