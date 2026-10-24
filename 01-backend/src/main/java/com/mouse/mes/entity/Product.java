package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Product {
    private Long id;
    private String productCode;
    private String productName;
    private String productType;
    private String model;
    private String specification;
    private String unit;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private String description;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}