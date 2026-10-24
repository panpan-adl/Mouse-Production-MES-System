package com.mouse.mes.service;

import com.mouse.mes.entity.Lot;

import java.util.List;

public interface LotService {
    List<Lot> findAll();
    Lot findById(Long id);
    Lot findByLotNo(String lotNo);
    Lot create(Lot lot);
    Lot update(Lot lot);
    void delete(Long id);
    List<Lot> findByMaterialId(Long materialId);
}