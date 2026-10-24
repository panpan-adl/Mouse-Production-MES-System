package com.mouse.mes.dto;

import lombok.Data;

@Data
public class FinanceQualityLossStat {
    private Double defectiveQuantity;
    private Double scrapQuantity;
    private Double reworkQuantity;
}
