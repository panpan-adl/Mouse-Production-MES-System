package com.mouse.mes.impl;

import com.mouse.mes.entity.Lot;
import com.mouse.mes.mapper.LotMapper;
import com.mouse.mes.service.LotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class LotServiceImpl implements LotService {

    @Autowired
    private LotMapper lotMapper;

    @Override
    public List<Lot> findAll() {
        return lotMapper.findAll();
    }

    @Override
    public Lot findById(Long id) {
        return lotMapper.findById(id);
    }

    @Override
    public Lot findByLotNo(String lotNo) {
        List<Lot> lots = lotMapper.findAll();
        for (Lot lot : lots) {
            if (lotNo.equals(lot.getLotNo())) {
                return lot;
            }
        }
        return null;
    }

    @Override
    public Lot create(Lot lot) {
        lot.setCreatedAt(new Date());
        lot.setUpdatedAt(new Date());
        lot.setIsDeleted(0);
        lotMapper.insert(lot);
        return lot;
    }

    @Override
    public Lot update(Lot lot) {
        lot.setUpdatedAt(new Date());
        lotMapper.update(lot);
        return lotMapper.findById(lot.getId());
    }

    @Override
    public void delete(Long id) {
        lotMapper.delete(id);
    }

    @Override
    public List<Lot> findByMaterialId(Long materialId) {
        return lotMapper.findByMaterialId(materialId);
    }
}