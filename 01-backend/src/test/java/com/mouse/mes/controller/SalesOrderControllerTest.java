package com.mouse.mes.controller;

import com.mouse.mes.entity.SalesOrder;
import com.mouse.mes.util.JsonResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SalesOrderControllerTest {

    @Autowired
    private SalesOrderController salesOrderController;

    @Test
    public void testAddSalesOrder() {
        JsonResult<SalesOrder> result = salesOrderController.create(buildSalesOrder("CTRL-ADD"));
        System.out.println("销售订单新增接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    public void testAddSalesOrderMissingOrderNo() {
        SalesOrder salesOrder = buildSalesOrder("CTRL-EMPTY-NO");
        salesOrder.setSoNo("");
        JsonResult<SalesOrder> result = salesOrderController.create(salesOrder);
        System.out.println("订单编号为空接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
    }

    @Test
    public void testAddSalesOrderMissingCustomerId() {
        SalesOrder salesOrder = buildSalesOrder("CTRL-EMPTY-CUST");
        salesOrder.setCustomerId(null);
        JsonResult<SalesOrder> result = salesOrderController.create(salesOrder);
        System.out.println("客户ID为空接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
    }

    @Test
    public void testGetAllSalesOrders() {
        JsonResult<List<SalesOrder>> result = salesOrderController.findAll();
        System.out.println("销售订单列表接口数量：" + result.getData().size());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    public void testGetSalesOrderById() {
        SalesOrder saved = salesOrderController.create(buildSalesOrder("CTRL-FIND")).getData();
        JsonResult<SalesOrder> result = salesOrderController.findById(saved.getId());
        System.out.println("销售订单查询接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals(saved.getSoNo(), result.getData().getSoNo());
    }

    @Test
    public void testGetSalesOrderByIdNotFound() {
        JsonResult<SalesOrder> result = salesOrderController.findById(-999999L);
        System.out.println("销售订单不存在接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
    }

    @Test
    public void testGetSalesOrderBySoNo() {
        SalesOrder saved = salesOrderController.create(buildSalesOrder("CTRL-FIND-NO")).getData();
        JsonResult<SalesOrder> result = salesOrderController.findBySoNo(saved.getSoNo());
        System.out.println("按订单号查询接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals(saved.getId(), result.getData().getId());
    }

    @Test
    public void testUpdateSalesOrder() {
        SalesOrder saved = salesOrderController.create(buildSalesOrder("CTRL-UPDATE")).getData();
        saved.setRemark("控制器修改销售订单");
        JsonResult<SalesOrder> result = salesOrderController.update(saved.getId(), saved);
        System.out.println("销售订单修改接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals("控制器修改销售订单", result.getData().getRemark());
    }

    @Test
    public void testDeleteSalesOrder() {
        SalesOrder saved = salesOrderController.create(buildSalesOrder("CTRL-DELETE")).getData();
        JsonResult<Void> result = salesOrderController.delete(saved.getId());
        JsonResult<SalesOrder> findResult = salesOrderController.findById(saved.getId());
        System.out.println("销售订单删除接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals(500, findResult.getCode());
    }

    private SalesOrder buildSalesOrder(String scene) {
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setSoNo("SO-" + scene + "-" + System.nanoTime());
        salesOrder.setCustomerId(1L);
        salesOrder.setOrderDate(new Date());
        salesOrder.setDeliveryDate(new Date());
        salesOrder.setTotalAmount(new BigDecimal("1000.00"));
        salesOrder.setStatus("PENDING");
        salesOrder.setRemark("销售订单控制器测试数据");
        salesOrder.setCreatedBy(1L);
        return salesOrder;
    }
}