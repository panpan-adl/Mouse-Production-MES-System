package com.mouse.mes.impl;

import com.mouse.mes.entity.InventoryIn;
import com.mouse.mes.mapper.InventoryInMapper;
import com.mouse.mes.service.InventoryInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class InventoryInServiceImpl implements InventoryInService {

    @Autowired
    private InventoryInMapper inventoryInMapper;

    @Override
    public List<InventoryIn> findAll() {
        return inventoryInMapper.findAll();
    }

    @Override
    public InventoryIn findById(Long id) {
        return inventoryInMapper.findById(id);
    }

    @Override
    public InventoryIn findByInNo(String inNo) {
        return inventoryInMapper.findByInNo(inNo);
    }

    @Override
    public InventoryIn create(InventoryIn inventoryIn) {
        inventoryIn.setCreateTime(new Date());
        inventoryInMapper.insert(inventoryIn);
        return inventoryIn;
    }

    @Override
    public InventoryIn update(InventoryIn inventoryIn) {
        inventoryInMapper.update(inventoryIn);
        return inventoryInMapper.findById(inventoryIn.getId());
    }
}