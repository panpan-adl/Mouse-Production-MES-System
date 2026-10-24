package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ProductionOrder {
    private Long id;
    private String poNo;
    private Long productId;
    private BigDecimal plannedQuantity;
    private BigDecimal actualQuantity;
    private Date plannedStartDate;
    private Date plannedEndDate;
    private Date actualStartDate;
    private Date actualEndDate;
    private String status;
    private Long departmentId;
    private Long createdBy;
    private Date createdAt;
    private Date updatedAt;
    private Integer isDeleted;
}
