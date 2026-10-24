package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class QualityStandard {
    private Long id;
    private Long productId;
    private String productName;
    private String standardName;
    private String standardContent;
    private BigDecimal qualifiedThreshold;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}