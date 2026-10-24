package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesOrderItem {
    private Long id;
    private Long soId;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String remark;
}
