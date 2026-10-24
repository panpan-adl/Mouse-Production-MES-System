package com.mouse.mes.mapper;

import com.mouse.mes.entity.QualityStandard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QualityStandardMapper {
    List<QualityStandard> findAll();
    QualityStandard findById(@Param("id") Long id);
    List<QualityStandard> findByProductId(@Param("productId") Long productId);
    int insert(QualityStandard qualityStandard);
    int update(QualityStandard qualityStandard);
    int delete(@Param("id") Long id);
}