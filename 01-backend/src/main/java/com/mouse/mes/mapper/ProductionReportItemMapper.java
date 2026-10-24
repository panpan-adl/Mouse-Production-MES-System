package com.mouse.mes.mapper;

import com.mouse.mes.entity.ProductionReportItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductionReportItemMapper {
    List<ProductionReportItem> findByReportId(@Param("reportId") Long reportId);
    int insert(ProductionReportItem productionReportItem);
    int deleteByReportId(@Param("reportId") Long reportId);
}
