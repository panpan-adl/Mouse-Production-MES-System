package com.mouse.mes.impl;

import com.mouse.mes.entity.QualityStandard;
import com.mouse.mes.mapper.QualityStandardMapper;
import com.mouse.mes.service.QualityStandardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class QualityStandardServiceImpl implements QualityStandardService {

    @Autowired
    private QualityStandardMapper qualityStandardMapper;

    @Override
    public List<QualityStandard> findAll() {
        return qualityStandardMapper.findAll();
    }

    @Override
    public QualityStandard findById(Long id) {
        return qualityStandardMapper.findById(id);
    }

    @Override
    public List<QualityStandard> findByProductId(Long productId) {
        return qualityStandardMapper.findByProductId(productId);
    }

    @Override
    public QualityStandard create(QualityStandard qualityStandard) {
        qualityStandard.setCreateTime(new Date());
        qualityStandard.setUpdateTime(new Date());
        if (qualityStandard.getStatus() == null) {
            qualityStandard.setStatus(1);
        }
        qualityStandardMapper.insert(qualityStandard);
        return qualityStandard;
    }

    @Override
    public QualityStandard update(QualityStandard qualityStandard) {
        qualityStandard.setUpdateTime(new Date());
        qualityStandardMapper.update(qualityStandard);
        return qualityStandardMapper.findById(qualityStandard.getId());
    }

    @Override
    public void delete(Long id) {
        qualityStandardMapper.delete(id);
    }
}