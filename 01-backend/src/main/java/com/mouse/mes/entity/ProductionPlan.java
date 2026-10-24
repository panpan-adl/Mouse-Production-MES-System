package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ProductionPlan {
    private Long id;
    private String planNo;
    private Long orderId;
    private Long productId;
    private String productName;
    private Integer planQuantity;
    private Integer completedQuantity;
    private Date startDate;
    private Date endDate;
    private Integer planStatus;
    private Integer priority;
    private Long createBy;
    private Date createTime;
    private Date updateTime;
}