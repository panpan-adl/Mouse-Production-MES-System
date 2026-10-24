package com.mouse.mes.dto;

import lombok.Data;

@Data
public class FinanceOverview {
    private Double salesIncome;
    private Double purchaseExpense;
    private Double maintenanceExpense;
    private Double inventoryAmount;
    private Double grossProfit;
}
