package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialIssueItem {
    private Long id;
    private Long issueId;
    private Long materialId;
    private Long lotId;
    private BigDecimal plannedQuantity;
    private BigDecimal actualQuantity;
    private Long locationId;
    private String remark;
}
