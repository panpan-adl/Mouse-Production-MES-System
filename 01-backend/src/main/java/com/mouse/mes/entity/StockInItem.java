package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockInItem {
    private Long id;
    private Long stockInId;
    private Long materialId;
    private Long lotId;
    private BigDecimal quantity;
    private Long locationId;
    private String remark;
}
