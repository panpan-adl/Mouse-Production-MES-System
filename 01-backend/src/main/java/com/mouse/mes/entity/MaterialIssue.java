package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class MaterialIssue {
    private Long id;
    private String issueNo;
    private Long workOrderId;
    private Long warehouseId;
    private BigDecimal totalQuantity;
    private String status;
    private String remark;
    private Long createdBy;
    private Date createdAt;
    private Date updatedAt;
    private Integer isDeleted;
}
