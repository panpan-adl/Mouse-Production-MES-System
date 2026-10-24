package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class InventoryOut {
    private Long id;
    private String outNo;
    private Integer outType;
    private Long materialId;
    private String materialName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Long warehouseId;
    private Date outDate;
    private Long createBy;
    private Date createTime;
    private String remark;
}