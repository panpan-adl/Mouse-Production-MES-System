package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductionReportItem {
    private Long id;
    private Long reportId;
    private Long productId;
    private BigDecimal goodQuantity;
    private BigDecimal defectiveQuantity;
    private String defectType;
    private String remark;
}
