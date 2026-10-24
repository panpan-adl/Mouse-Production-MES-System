package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class InventoryIn {
    private Long id;
    private String inNo;
    private Integer inType;
    private Long materialId;
    private String materialName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Long supplierId;
    private Long warehouseId;
    private Date inDate;
    private Long createBy;
    private Date createTime;
    private String remark;
}