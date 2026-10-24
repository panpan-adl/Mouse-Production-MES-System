package com.mouse.mes.impl;

import com.mouse.mes.entity.QualityCheck;
import com.mouse.mes.mapper.QualityCheckMapper;
import com.mouse.mes.service.QualityCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class QualityCheckServiceImpl implements QualityCheckService {

    @Autowired
    private QualityCheckMapper qualityCheckMapper;

    @Override
    public List<QualityCheck> findAll() {
        return qualityCheckMapper.findAll();
    }

    @Override
    public QualityCheck findById(Long id) {
        return qualityCheckMapper.findById(id);
    }

    @Override
    public List<QualityCheck> findByProductId(Long productId) {
        return qualityCheckMapper.findByProductId(productId);
    }

    @Override
    public List<QualityCheck> findByTaskId(Long taskId) {
        return qualityCheckMapper.findByTaskId(taskId);
    }

    @Override
    public QualityCheck create(QualityCheck qualityCheck) {
        qualityCheck.setCheckDate(new Date());
        if (qualityCheck.getCheckResult() == null) {
            qualityCheck.setCheckResult(0);
        }
        qualityCheckMapper.insert(qualityCheck);
        return qualityCheck;
    }

    @Override
    public QualityCheck update(QualityCheck qualityCheck) {
        qualityCheckMapper.update(qualityCheck);
        return qualityCheckMapper.findById(qualityCheck.getId());
    }
}