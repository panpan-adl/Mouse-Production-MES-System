package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Bom {
    private Long id;
    private Long productId;
    private Long materialId;
    private String materialName;
    private BigDecimal quantity;
    private String unit;
    private String remark;
}