package com.mouse.mes.service;

import com.mouse.mes.entity.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Test
    public void testFindAll() {
        List<Order> orders = orderService.findAll();
        assertNotNull(orders);
    }

    @Test
    public void testFindById() {
        List<Order> orders = orderService.findAll();
        if (!orders.isEmpty()) {
            Long id = orders.get(0).getId();
            Order order = orderService.findById(id);
            assertNotNull(order);
            assertEquals(id, order.getId());
        }
    }

    @Test
    public void testFindByIdNotFound() {
        Order order = orderService.findById(999999L);
        assertNull(order);
    }

    @Test
    public void testFindByOrderNo() {
        List<Order> orders = orderService.findAll();
        if (!orders.isEmpty()) {
            String orderNo = orders.get(0).getOrderNo();
            Order order = orderService.findByOrderNo(orderNo);
            assertNotNull(order);
            assertEquals(orderNo, order.getOrderNo());
        }
    }

    @Test
    public void testCreateOrder() {
        Order order = new Order();
        order.setOrderNo("SERVICE_TEST" + System.currentTimeMillis());
        order.setCustomerName("服务测试客户");
        order.setProductName("服务测试产品");
        order.setQuantity(100);
        order.setUnitPrice(new BigDecimal("10.00"));
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.setOrderDate(new Date());
        order.setDeliveryDate(new Date());
        order.setCreateBy(1L);

        Order created = orderService.create(order);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(order.getOrderNo(), created.getOrderNo());
        assertEquals(0, created.getOrderStatus());
        assertEquals(2, created.getPriority());
        assertNotNull(created.getCreateTime());
        assertNotNull(created.getUpdateTime());
    }

    @Test
    public void testConfirmOrder() {
        Order order = new Order();
        order.setOrderNo("CONFIRM_SERVICE" + System.currentTimeMillis());
        order.setCustomerName("确认服务测试");
        order.setProductName("测试产品");
        order.setQuantity(50);
        order.setUnitPrice(new BigDecimal("20.00"));
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.setOrderDate(new Date());
        order.setDeliveryDate(new Date());
        order.setCreateBy(1L);

        Order created = orderService.create(order);
        Order confirmed = orderService.confirmOrder(created.getId());

        assertNotNull(confirmed);
        assertEquals(1, confirmed.getOrderStatus());
    }

    @Test
    public void testConfirmOrderAlreadyConfirmed() {
        Order order = new Order();
        order.setOrderNo("DOUBLE_CONFIRM" + System.currentTimeMillis());
        order.setCustomerName("重复确认测试");
        order.setProductName("测试产品");
        order.setQuantity(30);
        order.setUnitPrice(new BigDecimal("15.00"));
        order.setTotalAmount(new BigDecimal("450.00"));
        order.setOrderDate(new Date());
        order.setDeliveryDate(new Date());
        order.setCreateBy(1L);

        Order created = orderService.create(order);
        orderService.confirmOrder(created.getId());

        assertThrows(RuntimeException.class, () -> orderService.confirmOrder(created.getId()));
    }

    @Test
    public void testCancelOrder() {
        Order order = new Order();
        order.setOrderNo("CANCEL_SERVICE" + System.currentTimeMillis());
        order.setCustomerName("取消服务测试");
        order.setProductName("测试产品");
        order.setQuantity(30);
        order.setUnitPrice(new BigDecimal("15.00"));
        order.setTotalAmount(new BigDecimal("450.00"));
        order.setOrderDate(new Date());
        order.setDeliveryDate(new Date());
        order.setCreateBy(1L);

        Order created = orderService.create(order);
        Order cancelled = orderService.cancelOrder(created.getId());

        assertNotNull(cancelled);
        assertEquals(4, cancelled.getOrderStatus());
    }

    @Test
    public void testFindByStatus() {
        List<Order> pendingOrders = orderService.findByStatus(0);
        assertNotNull(pendingOrders);
    }

    @Test
    public void testUpdateOrder() {
        Order order = new Order();
        order.setOrderNo("UPDATE_TEST" + System.currentTimeMillis());
        order.setCustomerName("更新测试");
        order.setProductName("测试产品");
        order.setQuantity(20);
        order.setUnitPrice(new BigDecimal("10.00"));
        order.setTotalAmount(new BigDecimal("200.00"));
        order.setOrderDate(new Date());
        order.setDeliveryDate(new Date());
        order.setCreateBy(1L);

        Order created = orderService.create(order);
        created.setCustomerName("更新后的客户");

        Order updated = orderService.update(created);
        assertNotNull(updated);
        assertEquals("更新后的客户", updated.getCustomerName());
    }
}