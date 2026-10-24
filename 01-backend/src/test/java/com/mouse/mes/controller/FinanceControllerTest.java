package com.mouse.mes.controller;

import com.mouse.mes.dto.FinanceGroupStat;
import com.mouse.mes.dto.FinanceInventoryValueStat;
import com.mouse.mes.dto.FinanceOverview;
import com.mouse.mes.dto.FinanceQualityLossStat;
import com.mouse.mes.dto.FinanceTrendStat;
import com.mouse.mes.entity.SalesOrder;
import com.mouse.mes.mapper.SalesOrderMapper;
import com.mouse.mes.util.JsonResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FinanceControllerTest {
    /**
     * 财务控制器，用于测试前端将要调用的财务看板接口。
     */
    @Autowired
    private FinanceController financeController;

    /**
     * 销售订单数据访问对象，用于准备控制器测试数据。
     */
    @Autowired
    private SalesOrderMapper salesOrderMapper;

    /**
     * 测试财务总览接口可以正常返回。
     */
    @Test
    public void testGetOverview() {
        JsonResult<FinanceOverview> result = financeController.getOverview();
        System.out.println("财务总览接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 测试按月份查询财务总览接口，新增销售订单后收入会增加。
     */
    @Test
    public void testGetOverviewByMonth() {
        String month = "2099-07";
        Double before = financeController.getOverviewByMonth(month).getData().getSalesIncome();
        insertSalesOrder(month, new BigDecimal("321.00"));
        JsonResult<FinanceOverview> result = financeController.getOverviewByMonth(month);
        Double increase = result.getData().getSalesIncome() - before;
        System.out.println("按月财务总览接口结果：" + result.getData());
        assertEquals(200, result.getCode());
        assertTrue(Math.abs(increase - 321.00) < 0.01, "金额不一致");
    }

    /**
     * 测试无数据月份接口返回0，不应报错。
     */
    @Test
    public void testNoDataMonthReturnZero() {
        JsonResult<FinanceOverview> result = financeController.getOverviewByMonth("2999-12");
        System.out.println("无数据月份财务接口结果：" + result.getData());
        assertEquals(200, result.getCode());
        assertEquals(0.0, result.getData().getSalesIncome(), 0.01);
        assertEquals(0.0, result.getData().getPurchaseExpense(), 0.01);
        assertEquals(0.0, result.getData().getMaintenanceExpense(), 0.01);
        assertEquals(0.0, result.getData().getGrossProfit(), 0.01);
    }

    /**
     * 测试月份格式错误时接口返回失败。
     */
    @Test
    public void testInvalidMonthShouldFail() {
        JsonResult<FinanceOverview> result = financeController.getOverviewByMonth("2099");
        System.out.println("错误月份格式接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
        assertEquals("月份格式必须为yyyy-MM", result.getMsg());
    }

    /**
     * 测试采购统计接口可以正常返回。
     */
    @Test
    public void testPurchaseStatsCanReturn() {
        JsonResult<List<FinanceGroupStat>> monthly = financeController.getPurchaseAmountByMonth();
        JsonResult<List<FinanceGroupStat>> supplier = financeController.getPurchaseAmountBySupplier();
        JsonResult<List<FinanceGroupStat>> material = financeController.getPurchaseAmountByMaterial();
        System.out.println("采购统计接口结果：" + monthly.getMsg());
        assertEquals(200, monthly.getCode());
        assertEquals(200, supplier.getCode());
        assertEquals(200, material.getCode());
        assertNotNull(monthly.getData());
        assertNotNull(supplier.getData());
        assertNotNull(material.getData());
    }

    /**
     * 测试库存、质检损失和趋势接口可以正常返回。
     */
    @Test
    public void testBoardStatsCanReturn() {
        JsonResult<FinanceInventoryValueStat> inventory = financeController.getInventoryValueStat();
        JsonResult<FinanceQualityLossStat> qualityLoss = financeController.getQualityLossStat();
        JsonResult<List<FinanceTrendStat>> trend = financeController.getMonthlyTrend();
        System.out.println("财务看板扩展接口结果：" + inventory.getMsg());
        assertEquals(200, inventory.getCode());
        assertEquals(200, qualityLoss.getCode());
        assertEquals(200, trend.getCode());
        assertNotNull(inventory.getData());
        assertNotNull(qualityLoss.getData());
        assertNotNull(trend.getData());
    }

    /**
     * 插入销售订单测试数据。
     */
    private void insertSalesOrder(String month, BigDecimal amount) {
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setSoNo("FIN-CTRL-SO-" + month + "-" + System.nanoTime());
        salesOrder.setCustomerId(1L);
        salesOrder.setOrderDate(dateOf(month));
        salesOrder.setDeliveryDate(dateOf(month));
        salesOrder.setTotalAmount(amount);
        salesOrder.setStatus("COMPLETED");
        salesOrder.setRemark("财务控制器测试销售订单");
        salesOrder.setCreatedBy(1L);
        salesOrder.setCreatedAt(new Date());
        salesOrder.setUpdatedAt(new Date());
        salesOrder.setIsDeleted(0);
        salesOrderMapper.insert(salesOrder);
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
