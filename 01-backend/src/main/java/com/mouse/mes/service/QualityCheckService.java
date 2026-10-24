package com.mouse.mes.service;

import com.mouse.mes.entity.QualityCheck;

import java.util.List;

public interface QualityCheckService {
    List<QualityCheck> findAll();
    QualityCheck findById(Long id);
    List<QualityCheck> findByProductId(Long productId);
    List<QualityCheck> findByTaskId(Long taskId);
    QualityCheck create(QualityCheck qualityCheck);
    QualityCheck update(QualityCheck qualityCheck);
}