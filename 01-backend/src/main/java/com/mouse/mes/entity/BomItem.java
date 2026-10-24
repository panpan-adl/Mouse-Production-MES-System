package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BomItem {
    private Long id;
    private Long bomId;
    private Long materialId;
    private BigDecimal quantity;
    private String remark;
}
