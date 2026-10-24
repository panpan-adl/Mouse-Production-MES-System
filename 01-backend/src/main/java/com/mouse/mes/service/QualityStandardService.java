package com.mouse.mes.service;

import com.mouse.mes.entity.QualityStandard;

import java.util.List;

public interface QualityStandardService {
    List<QualityStandard> findAll();
    QualityStandard findById(Long id);
    List<QualityStandard> findByProductId(Long productId);
    QualityStandard create(QualityStandard qualityStandard);
    QualityStandard update(QualityStandard qualityStandard);
    void delete(Long id);
}