package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class QualityCheck {
    private Long id;
    private String checkNo;
    private Long taskId;
    private Long productId;
    private String productName;
    private Integer checkQuantity;
    private Integer qualifiedQuantity;
    private Integer defectQuantity;
    private BigDecimal qualifiedRate;
    private Integer checkResult;
    private Long checkerId;
    private String checkerName;
    private Date checkDate;
    private String defectDesc;
    private String handleResult;
    private Date createTime;
}