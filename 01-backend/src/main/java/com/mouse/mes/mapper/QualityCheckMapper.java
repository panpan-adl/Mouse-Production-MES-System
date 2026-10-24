package com.mouse.mes.mapper;

import com.mouse.mes.entity.QualityCheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QualityCheckMapper {
    List<QualityCheck> findAll();
    QualityCheck findById(@Param("id") Long id);
    QualityCheck findByCheckNo(@Param("checkNo") String checkNo);
    List<QualityCheck> findByTaskId(@Param("taskId") Long taskId);
    List<QualityCheck> findByProductId(@Param("productId") Long productId);
    int insert(QualityCheck qualityCheck);
    int update(QualityCheck qualityCheck);
}