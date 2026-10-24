package com.mouse.mes.service;

import com.mouse.mes.dto.FinanceGroupStat;
import com.mouse.mes.dto.FinanceInventoryValueStat;
import com.mouse.mes.dto.FinanceOverview;
import com.mouse.mes.dto.FinanceQualityLossStat;
import com.mouse.mes.dto.FinanceTrendStat;

import java.util.List;

public interface FinanceService {
    /**
     * 查询财务总览数据。
     */
    FinanceOverview getOverview();

    /**
     * 按月份查询财务总览数据，月份格式为yyyy-MM。
     */
    FinanceOverview getOverviewByMonth(String month);

    /**
     * 按月份统计采购金额。
     */
    List<FinanceGroupStat> getPurchaseAmountByMonth();

    /**
     * 按供应商统计采购金额。
     */
    List<FinanceGroupStat> getPurchaseAmountBySupplier();

    /**
     * 按物料统计采购金额。
     */
    List<FinanceGroupStat> getPurchaseAmountByMaterial();

    /**
     * 查询库存数量、库存金额和低库存物料数量。
     */
    FinanceInventoryValueStat getInventoryValueStat();

    /**
     * 查询质检损失统计。
     */
    FinanceQualityLossStat getQualityLossStat();

    /**
     * 查询月度财务趋势数据。
     */
    List<FinanceTrendStat> getMonthlyTrend();

    /**
     * 按客户统计销售收入。
     */
    List<FinanceGroupStat> getSalesIncomeByCustomer();

    /**
     * 按设备统计维修成本。
     */
    List<FinanceGroupStat> getMaintenanceCostByEquipment();
}
