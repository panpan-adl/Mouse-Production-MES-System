package com.mouse.mes.mapper;

import com.mouse.mes.entity.ProductionReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductionReportMapper {
    List<ProductionReport> findAll();
    ProductionReport findById(@Param("id") Long id);
    List<ProductionReport> findByTaskId(@Param("taskId") Long taskId);
    int insert(ProductionReport productionReport);
    int update(ProductionReport productionReport);
    List<ProductionReport> findByReportUserId(@Param("reportUserId") Long reportUserId);
    ProductionReport getTaskSummary(@Param("taskId") Long taskId);
}