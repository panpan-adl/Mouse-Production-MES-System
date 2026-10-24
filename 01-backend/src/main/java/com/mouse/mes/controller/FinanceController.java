package com.mouse.mes.controller;

import com.mouse.mes.dto.FinanceGroupStat;
import com.mouse.mes.dto.FinanceInventoryValueStat;
import com.mouse.mes.dto.FinanceOverview;
import com.mouse.mes.dto.FinanceQualityLossStat;
import com.mouse.mes.dto.FinanceTrendStat;
import com.mouse.mes.service.FinanceService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {
    /**
     * 财务业务服务，用于获取财务看板和统计图数据。
     */
    @Autowired
    private FinanceService financeService;

    /**
     * 查询财务总览。
     */
    @GetMapping("/overview")
    public JsonResult<FinanceOverview> getOverview() {
        return JsonResult.success(financeService.getOverview());
    }

    /**
     * 按月份查询财务总览，月份格式为yyyy-MM。
     */
    @GetMapping("/overview/month/{month}")
    public JsonResult<FinanceOverview> getOverviewByMonth(@PathVariable String month) {
        try {
            return JsonResult.success(financeService.getOverviewByMonth(month));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 查询按月份统计的采购金额。
     */
    @GetMapping("/purchase/monthly")
    public JsonResult<List<FinanceGroupStat>> getPurchaseAmountByMonth() {
        return JsonResult.success(financeService.getPurchaseAmountByMonth());
    }

    /**
     * 查询按供应商统计的采购金额。
     */
    @GetMapping("/purchase/supplier")
    public JsonResult<List<FinanceGroupStat>> getPurchaseAmountBySupplier() {
        return JsonResult.success(financeService.getPurchaseAmountBySupplier());
    }

    /**
     * 查询按物料统计的采购金额。
     */
    @GetMapping("/purchase/material")
    public JsonResult<List<FinanceGroupStat>> getPurchaseAmountByMaterial() {
        return JsonResult.success(financeService.getPurchaseAmountByMaterial());
    }

    /**
     * 查询库存价值统计。
     */
    @GetMapping("/inventory/value")
    public JsonResult<FinanceInventoryValueStat> getInventoryValueStat() {
        return JsonResult.success(financeService.getInventoryValueStat());
    }

    /**
     * 查询质检损失统计。
     */
    @GetMapping("/quality/loss")
    public JsonResult<FinanceQualityLossStat> getQualityLossStat() {
        return JsonResult.success(financeService.getQualityLossStat());
    }

    /**
     * 查询财务趋势图数据。
     */
    @GetMapping("/trend/monthly")
    public JsonResult<List<FinanceTrendStat>> getMonthlyTrend() {
        return JsonResult.success(financeService.getMonthlyTrend());
    }

    /**
     * 查询按客户统计的销售收入。
     */
    @GetMapping("/sales/customer")
    public JsonResult<List<FinanceGroupStat>> getSalesIncomeByCustomer() {
        return JsonResult.success(financeService.getSalesIncomeByCustomer());
    }

    /**
     * 查询按设备统计的维修成本。
     */
    @GetMapping("/maintenance/equipment")
    public JsonResult<List<FinanceGroupStat>> getMaintenanceCostByEquipment() {
        return JsonResult.success(financeService.getMaintenanceCostByEquipment());
    }
}
