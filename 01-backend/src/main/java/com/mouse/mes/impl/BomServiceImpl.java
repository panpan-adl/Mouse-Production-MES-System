package com.mouse.mes.impl;

import com.mouse.mes.entity.Bom;
import com.mouse.mes.mapper.BomMapper;
import com.mouse.mes.service.BomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BomServiceImpl implements BomService {

    @Autowired
    private BomMapper bomMapper;

    @Override
    public List<Bom> findAll() {
        return bomMapper.findAll();
    }

    @Override
    public Bom findById(Long id) {
        return bomMapper.findById(id);
    }

    @Override
    public List<Bom> findByProductId(Long productId) {
        return bomMapper.findByProductId(productId);
    }

    @Override
    public Bom create(Bom bom) {
        bomMapper.insert(bom);
        return bom;
    }

    @Override
    public Bom update(Bom bom) {
        bomMapper.update(bom);
        return bomMapper.findById(bom.getId());
    }

    @Override
    public void delete(Long id) {
        bomMapper.delete(id);
    }
}