package com.mouse.mes.dto;

import lombok.Data;

@Data
public class FinanceInventoryValueStat {
    private Double currentQuantity;
    private Double currentAmount;
    private Integer lowStockCount;
}
