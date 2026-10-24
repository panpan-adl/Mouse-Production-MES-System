package com.mouse.mes.service;

import com.mouse.mes.dto.FinanceOverview;
import com.mouse.mes.entity.Maintenance;
import com.mouse.mes.entity.PurchaseOrder;
import com.mouse.mes.entity.SalesOrder;
import com.mouse.mes.mapper.MaintenanceMapper;
import com.mouse.mes.mapper.PurchaseOrderMapper;
import com.mouse.mes.mapper.SalesOrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class FinanceServiceTest {
    /**
     * 财务业务服务，用于测试财务总览、月份统计和利润计算。
     */
    @Autowired
    private FinanceService financeService;

    /**
     * 采购订单数据访问对象，用于准备采购支出测试数据。
     */
    @Autowired
    private PurchaseOrderMapper purchaseOrderMapper;

    /**
     * 销售订单数据访问对象，用于准备销售收入测试数据。
     */
    @Autowired
    private SalesOrderMapper salesOrderMapper;

    /**
     * 维修单数据访问对象，用于准备维修成本测试数据。
     */
    @Autowired
    private MaintenanceMapper maintenanceMapper;

    /**
     * 测试有采购订单时，采购支出统计应增加对应金额。
     */
    @Test
    public void testPurchaseExpenseWithPurchaseOrder() {
        String month = "2099-01";
        FinanceOverview before = financeService.getOverviewByMonth(month);
        insertPurchaseOrder(month, new BigDecimal("123.45"));
        FinanceOverview after = financeService.getOverviewByMonth(month);
        System.out.println("采购支出统计结果：" + after.getPurchaseExpense());
        assertTrue(Math.abs((after.getPurchaseExpense() - before.getPurchaseExpense()) - 123.45) < 0.01, "金额不一致");
    }

    /**
     * 测试有销售订单时，销售收入统计应增加对应金额。
     */
    @Test
    public void testSalesIncomeWithSalesOrder() {
        String month = "2099-02";
        FinanceOverview before = financeService.getOverviewByMonth(month);
        insertSalesOrder(month, new BigDecimal("500.00"));
        FinanceOverview after = financeService.getOverviewByMonth(month);
        System.out.println("销售收入统计结果：" + after.getSalesIncome());
        assertTrue(Math.abs((after.getSalesIncome() - before.getSalesIncome()) - 500.00) < 0.01, "金额不一致");
    }

    /**
     * 测试有已完成维修记录时，维修成本统计应增加对应金额。
     */
    @Test
    public void testMaintenanceExpenseWithMaintenanceRecord() {
        String month = "2099-03";
        FinanceOverview before = financeService.getOverviewByMonth(month);
        insertMaintenance(month, new BigDecimal("66.66"));
        FinanceOverview after = financeService.getOverviewByMonth(month);
        System.out.println("维修支出统计结果：" + after.getMaintenanceExpense());
        assertTrue(Math.abs((after.getMaintenanceExpense() - before.getMaintenanceExpense()) - 66.66) < 0.01, "金额不一致");
    }

    /**
     * 测试利润公式：利润 = 收入 - 采购支出 - 维修支出。
     */
    @Test
    public void testProfitEqualsIncomeMinusPurchaseAndMaintenance() {
        String month = "2099-04";
        FinanceOverview before = financeService.getOverviewByMonth(month);
        insertSalesOrder(month, new BigDecimal("1000.00"));
        insertPurchaseOrder(month, new BigDecimal("300.00"));
        insertMaintenance(month, new BigDecimal("100.00"));
        FinanceOverview after = financeService.getOverviewByMonth(month);
        Double profitDelta = after.getGrossProfit() - before.getGrossProfit();
        System.out.println("利润统计结果：" + after.getGrossProfit());
        assertTrue(Math.abs(profitDelta - 600.00) < 0.01, "金额不一致");
    }

    /**
     * 测试无销售、采购、维修数据的月份返回0，不应报错。
     */
    @Test
    public void testNoDataMonthReturnZero() {
        FinanceOverview overview = financeService.getOverviewByMonth("2999-12");
        System.out.println("无数据月份财务总览：" + overview);
        assertEquals(0.0, overview.getSalesIncome(), 0.01);
        assertEquals(0.0, overview.getPurchaseExpense(), 0.01);
        assertEquals(0.0, overview.getMaintenanceExpense(), 0.01);
        assertEquals(0.0, overview.getGrossProfit(), 0.01);
    }

    /**
     * 测试按月份查询时，只统计指定月份的数据，不统计其他月份。
     */
    @Test
    public void testMonthQueryOnlyCountsSelectedMonth() {
        String targetMonth = "2099-05";
        String otherMonth = "2099-06";
        FinanceOverview before = financeService.getOverviewByMonth(targetMonth);
        insertSalesOrder(targetMonth, new BigDecimal("200.00"));
        insertSalesOrder(otherMonth, new BigDecimal("900.00"));
        FinanceOverview after = financeService.getOverviewByMonth(targetMonth);
        System.out.println("指定月份销售收入统计结果：" + after.getSalesIncome());
        assertTrue(Math.abs((after.getSalesIncome() - before.getSalesIncome()) - 200.00) < 0.01, "金额不一致");
    }

    /**
     * 测试财务总览、库存统计、质检损失和趋势数据都能正常返回。
     */
    @Test
    public void testOtherFinanceStatsCanReturn() {
        assertNotNull(financeService.getOverview());
        assertNotNull(financeService.getInventoryValueStat());
        assertNotNull(financeService.getQualityLossStat());
        assertNotNull(financeService.getPurchaseAmountByMonth());
        assertNotNull(financeService.getPurchaseAmountBySupplier());
        assertNotNull(financeService.getPurchaseAmountByMaterial());
        assertNotNull(financeService.getMonthlyTrend());
    }

    /**
     * 插入采购订单测试数据。
     */
    private void insertPurchaseOrder(String month, BigDecimal amount) {
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setPoNo("FIN-PO-" + month + "-" + System.nanoTime());
        purchaseOrder.setSupplierId(1L);
        purchaseOrder.setOrderDate(dateOf(month));
        purchaseOrder.setExpectedDate(dateOf(month));
        purchaseOrder.setTotalAmount(amount);
        purchaseOrder.setStatus("COMPLETED");
        purchaseOrder.setRemark("财务测试采购订单");
        purchaseOrder.setCreatedBy(1L);
        purchaseOrder.setCreatedAt(new Date());
        purchaseOrder.setUpdatedAt(new Date());
        purchaseOrder.setIsDeleted(0);
        purchaseOrderMapper.insert(purchaseOrder);
    }

    /**
     * 插入销售订单测试数据。
     */
    private void insertSalesOrder(String month, BigDecimal amount) {
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setSoNo("FIN-SO-" + month + "-" + System.nanoTime());
        salesOrder.setCustomerId(1L);
        salesOrder.setOrderDate(dateOf(month));
        salesOrder.setDeliveryDate(dateOf(month));
        salesOrder.setTotalAmount(amount);
        salesOrder.setStatus("COMPLETED");
        salesOrder.setRemark("财务测试销售订单");
        salesOrder.setCreatedBy(1L);
        salesOrder.setCreatedAt(new Date());
        salesOrder.setUpdatedAt(new Date());
        salesOrder.setIsDeleted(0);
        salesOrderMapper.insert(salesOrder);
    }

    /**
     * 插入已完成维修单测试数据。
     */
    private void insertMaintenance(String month, BigDecimal cost) {
        Maintenance maintenance = new Maintenance();
        maintenance.setMaintenanceNo("FIN-MT-" + month + "-" + System.nanoTime());
        maintenance.setEquipmentId(1L);
        maintenance.setEquipmentName("财务测试设备");
        maintenance.setMaintenanceType(2);
        maintenance.setFaultDesc("财务测试故障");
        maintenance.setMaintenanceContent("财务测试维修内容");
        maintenance.setMaintenanceUserId(1L);
        maintenance.setMaintenanceUserName("财务测试维修人员");
        maintenance.setStartTime(dateOf(month));
        maintenance.setEndTime(dateOf(month));
        maintenance.setCost(cost);
        maintenance.setResult("REPAIRED");
        maintenance.setStatus("FINISHED");
        maintenance.setRemark("财务测试维修单");
        maintenance.setCreateTime(dateOf(month));
        maintenance.setUpdateTime(new Date());
        maintenanceMapper.insert(maintenance);
    }

    /**
     * 根据yyyy-MM构造该月第一天的日期。
     */
    private Date dateOf(String month) {
        LocalDate localDate = LocalDate.parse(month + "-01");
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 比较金额是否相等，忽略BigDecimal的小数位长度差异。
     */
    private void assertAmountEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual), "金额不一致，期望：" + expected + "，实际：" + actual);
    }
}
