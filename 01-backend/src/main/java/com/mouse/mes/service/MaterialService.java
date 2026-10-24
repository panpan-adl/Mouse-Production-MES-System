package com.mouse.mes.service;

import com.mouse.mes.entity.Material;

import java.util.List;

public interface MaterialService {
    List<Material> findAll();
    List<Material> findByTypes(List<Integer> types);
    Material findById(Long id);
    Material findByMaterialCode(String materialCode);
    Material create(Material material);
    Material update(Material material);
    void delete(Long id);
    void updateStock(Long id, Integer quantity);
}