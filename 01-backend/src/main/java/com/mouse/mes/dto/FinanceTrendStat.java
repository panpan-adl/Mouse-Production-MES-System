package com.mouse.mes.dto;

import lombok.Data;

@Data
public class FinanceTrendStat {
    private String month;
    private Double salesIncome;
    private Double purchaseExpense;
    private Double maintenanceExpense;
    private Double profit;
}
