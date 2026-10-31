package com.mouse.mes.impl;

import com.mouse.mes.entity.Material;
import com.mouse.mes.mapper.MaterialMapper;
import com.mouse.mes.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class MaterialServiceImpl implements MaterialService {

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired private com.mouse.mes.service.MaterialOriginService materialOrigin;
    @Autowired private com.mouse.mes.service.FulfillmentService fulfillment;

    @Override
    public List<Material> findAll() {
        return materialMapper.findAll();
    }

    @Override
    public List<Material> findByTypes(List<Integer> types) {
        return materialMapper.findByTypes(types);
    }

    @Override
    public Material findById(Long id) {
        return materialMapper.findById(id);
    }

    @Override
    public Material findByMaterialCode(String materialCode) {
        return materialMapper.findByMaterialCode(materialCode);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Material create(Material material) {
        fulfillment.lock();
        material.setCreateTime(new Date());
        material.setUpdateTime(new Date());
        if (material.getStatus() == null) {
            material.setStatus(1);
        }
        if (material.getCurrentStock() == null) {
            material.setCurrentStock(0);
        }
        materialMapper.insert(material);
        materialOrigin.created(material.getId());
        return material;
    }

    @Override
    public Material update(Material material) {
        material.setUpdateTime(new Date());
        materialMapper.update(material);
        return materialMapper.findById(material.getId());
    }

    @Override
    public void delete(Long id) {
        materialMapper.delete(id);
    }

    @Override
    public void updateStock(Long id, Integer quantity) {
        materialMapper.updateStock(id, quantity);
    }
}