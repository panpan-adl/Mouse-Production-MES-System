package com.mouse.mes.service;

import com.mouse.mes.entity.Bom;

import java.util.List;

public interface BomService {
    List<Bom> findAll();
    Bom findById(Long id);
    List<Bom> findByProductId(Long productId);
    Bom create(Bom bom);
    Bom update(Bom bom);
    void delete(Long id);
}