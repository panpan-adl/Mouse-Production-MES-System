package com.mouse.mes.impl;

import com.mouse.mes.entity.InventoryOut;
import com.mouse.mes.mapper.InventoryOutMapper;
import com.mouse.mes.service.InventoryOutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class InventoryOutServiceImpl implements InventoryOutService {

    @Autowired
    private InventoryOutMapper inventoryOutMapper;

    @Override
    public List<InventoryOut> findAll() {
        return inventoryOutMapper.findAll();
    }

    @Override
    public InventoryOut findById(Long id) {
        return inventoryOutMapper.findById(id);
    }

    @Override
    public InventoryOut findByOutNo(String outNo) {
        return inventoryOutMapper.findByOutNo(outNo);
    }

    @Override
    public InventoryOut create(InventoryOut inventoryOut) {
        inventoryOut.setCreateTime(new Date());
        inventoryOutMapper.insert(inventoryOut);
        return inventoryOut;
    }

    @Override
    public InventoryOut update(InventoryOut inventoryOut) {
        inventoryOutMapper.update(inventoryOut);
        return inventoryOutMapper.findById(inventoryOut.getId());
    }
}