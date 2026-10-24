package com.mouse.mes.service;

import com.mouse.mes.entity.Order;
import com.mouse.mes.entity.ProductionPlan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ProductionPlanServiceTest {

    @Autowired
    private ProductionPlanService productionPlanService;

    @Autowired
    private OrderService orderService;

    @Test
    public void testFindAll() {
        List<ProductionPlan> plans = productionPlanService.findAll();
        assertNotNull(plans);
    }

    @Test
    public void testFindById() {
        List<ProductionPlan> plans = productionPlanService.findAll();
        if (!plans.isEmpty()) {
            Long id = plans.get(0).getId();
            ProductionPlan plan = productionPlanService.findById(id);
            assertNotNull(plan);
            assertEquals(id, plan.getId());
        }
    }

    @Test
    public void testFindByPlanNo() {
        List<ProductionPlan> plans = productionPlanService.findAll();
        if (!plans.isEmpty()) {
            String planNo = plans.get(0).getPlanNo();
            ProductionPlan plan = productionPlanService.findByPlanNo(planNo);
            assertNotNull(plan);
            assertEquals(planNo, plan.getPlanNo());
        }
    }

    @Test
    public void testCreateProductionPlan() {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNo("PP_SERVICE_TEST" + System.currentTimeMillis());
        plan.setProductId(1L);
        plan.setProductName("服务测试产品");
        plan.setPlanQuantity(100);
        plan.setCreateBy(1L);

        ProductionPlan created = productionPlanService.create(plan);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(plan.getPlanNo(), created.getPlanNo());
        assertEquals(0, created.getPlanStatus());
        assertEquals(0, created.getCompletedQuantity());
        assertNotNull(created.getCreateTime());
        assertNotNull(created.getUpdateTime());
    }

    @Test
    public void testCreateFromOrder() {
        Order order = new Order();
        order.setOrderNo("ORDER_PP_TEST" + System.currentTimeMillis());
        order.setCustomerName("计划服务测试客户");
        order.setProductName("计划服务产品");
        order.setQuantity(200);
        order.setUnitPrice(new BigDecimal("10.00"));
        order.setTotalAmount(new BigDecimal("2000.00"));
        order.setOrderDate(new Date());
        order.setDeliveryDate(new Date());
        order.setCreateBy(1L);

        Order createdOrder = orderService.create(order);
        Order confirmedOrder = orderService.confirmOrder(createdOrder.getId());

        ProductionPlan plan = productionPlanService.createFromOrder(confirmedOrder.getId());

        assertNotNull(plan);
        assertNotNull(plan.getId());
        assertEquals(confirmedOrder.getId(), plan.getOrderId());
        assertEquals(confirmedOrder.getProductName(), plan.getProductName());
        assertEquals(confirmedOrder.getQuantity(), plan.getPlanQuantity());
        assertEquals(0, plan.getPlanStatus());
        assertNotNull(plan.getPlanNo());
    }

    @Test
    public void testCreateFromOrderNotConfirmed() {
        Order order = new Order();
        order.setOrderNo("ORDER_PP_UNCONF" + System.currentTimeMillis());
        order.setCustomerName("未确认计划测试");
        order.setProductName("测试产品");
        order.setQuantity(100);
        order.setUnitPrice(new BigDecimal("10.00"));
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.setOrderDate(new Date());
        order.setDeliveryDate(new Date());
        order.setCreateBy(1L);

        Order createdOrder = orderService.create(order);

        assertThrows(RuntimeException.class, () -> productionPlanService.createFromOrder(createdOrder.getId()));
    }

    @Test
    public void testGetProgress() {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNo("PP_PROGRESS" + System.currentTimeMillis());
        plan.setProductId(1L);
        plan.setProductName("进度测试产品");
        plan.setPlanQuantity(100);
        plan.setCreateBy(1L);

        ProductionPlan created = productionPlanService.create(plan);
        ProductionPlan progress = productionPlanService.getProgress(created.getId());

        assertNotNull(progress);
        assertEquals(created.getId(), progress.getId());
    }

    @Test
    public void testGetProgressNotFound() {
        assertThrows(RuntimeException.class, () -> productionPlanService.getProgress(999999L));
    }

    @Test
    public void testFindByStatus() {
        List<ProductionPlan> pendingPlans = productionPlanService.findByStatus(0);
        assertNotNull(pendingPlans);
    }

    @Test
    public void testFindByOrderId() {
        List<ProductionPlan> plans = productionPlanService.findByOrderId(1L);
        assertNotNull(plans);
    }

    @Test
    public void testUpdateProductionPlan() {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNo("PP_UPDATE" + System.currentTimeMillis());
        plan.setProductId(1L);
        plan.setProductName("更新测试产品");
        plan.setPlanQuantity(100);
        plan.setCreateBy(1L);

        ProductionPlan created = productionPlanService.create(plan);
        created.setPlanQuantity(150);

        ProductionPlan updated = productionPlanService.update(created);
        assertNotNull(updated);
        assertEquals(150, updated.getPlanQuantity());
    }
}