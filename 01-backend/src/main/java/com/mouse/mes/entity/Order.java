package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Order {
    private Long id;
    private String orderNo;
    private String customerName;
    private String customerPhone;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Date orderDate;
    private Date deliveryDate;
    private Integer orderStatus;
    private Integer priority;
    private String remark;
    private Long createBy;
    private Date createTime;
    private Date updateTime;
    private Integer deleted;
}