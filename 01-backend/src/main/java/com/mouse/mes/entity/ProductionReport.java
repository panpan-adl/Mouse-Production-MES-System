package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ProductionReport {
    private Long id;
    private Long taskId;
    private Long reportUserId;
    private String reportUserName;
    private Integer reportQuantity;
    private Integer qualifiedQuantity;
    private Integer defectQuantity;
    private Date reportTime;
    private String remark;
}