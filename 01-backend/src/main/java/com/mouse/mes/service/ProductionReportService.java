package com.mouse.mes.service;

import com.mouse.mes.entity.ProductionReport;

import java.util.List;

public interface ProductionReportService {
    List<ProductionReport> findAll();
    ProductionReport findById(Long id);
    List<ProductionReport> findByTaskId(Long taskId);
    ProductionReport create(ProductionReport productionReport);
    ProductionReport update(Long id, ProductionReport productionReport);
    List<ProductionReport> findByReportUserId(Long reportUserId);
    ProductionReport getTaskSummary(Long taskId);
}