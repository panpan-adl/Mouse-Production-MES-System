package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class InventoryTransaction {
    private Long id;
    private String transactionNo;
    private String transactionType;
    private String businessType;
    private Long sourceOrderId;
    private String sourceOrderNo;
    private Long warehouseId;
    private Long locationId;
    private Long materialId;
    private Long lotId;
    private BigDecimal quantity;
    private BigDecimal beforeQuantity;
    private BigDecimal afterQuantity;
    private Long createdBy;
    private Date createdAt;
    private String remark;
}