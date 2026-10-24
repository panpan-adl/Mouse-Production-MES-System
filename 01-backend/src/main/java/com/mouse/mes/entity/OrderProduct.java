package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderProduct {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String remark;
}