package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Inventory {
    private Long id;
    private Long warehouseId;
    private Long locationId;
    private Long materialId;
    private Long lotId;
    private BigDecimal quantity;
    private Date lastUpdateTime;
    private Integer isDeleted;
}
