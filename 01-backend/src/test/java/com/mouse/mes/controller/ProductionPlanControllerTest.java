package com.mouse.mes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.entity.Order;
import com.mouse.mes.entity.ProductionPlan;
import com.mouse.mes.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductionPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderService orderService;

    @Test
    public void testGetAllProductionPlans() throws Exception {
        mockMvc.perform(get("/api/production-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetProductionPlanById() throws Exception {
        mockMvc.perform(get("/api/production-plans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testCreateProductionPlan() throws Exception {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNo("PLAN_TEST" + System.currentTimeMillis());
        plan.setProductId(1L);
        plan.setProductName("测试计划产品");
        plan.setPlanQuantity(100);
        plan.setCreateBy(1L);

        mockMvc.perform(post("/api/production-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.planNo").value(plan.getPlanNo()))
                .andExpect(jsonPath("$.data.planStatus").value(0));
    }

    @Test
    public void testCreateFromOrder() throws Exception {
        Order order = new Order();
        order.setOrderNo("ORDER_FOR_PLAN" + System.currentTimeMillis());
        order.setCustomerName("计划测试客户");
        order.setProductName("计划产品");
        order.setQuantity(100);
        order.setUnitPrice(new BigDecimal("10.00"));
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.setOrderDate(new Date());
        order.setDeliveryDate(new Date());
        order.setCreateBy(1L);

        Order createdOrder = orderService.create(order);
        Order confirmedOrder = orderService.confirmOrder(createdOrder.getId());

        mockMvc.perform(post("/api/production-plans/from-order/" + confirmedOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value(confirmedOrder.getId()))
                .andExpect(jsonPath("$.data.productName").value(confirmedOrder.getProductName()))
                .andExpect(jsonPath("$.data.planQuantity").value(confirmedOrder.getQuantity()));
    }

    @Test
    public void testCreateFromOrderNotConfirmed() throws Exception {
        Order order = new Order();
        order.setOrderNo("UNCONFIRMED_PLAN" + System.currentTimeMillis());
        order.setCustomerName("未确认测试");
        order.setProductName("计划产品");
        order.setQuantity(50);
        order.setUnitPrice(new BigDecimal("10.00"));
        order.setTotalAmount(new BigDecimal("500.00"));
        order.setOrderDate(new Date());
        order.setDeliveryDate(new Date());
        order.setCreateBy(1L);

        Order createdOrder = orderService.create(order);

        mockMvc.perform(post("/api/production-plans/from-order/" + createdOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    public void testGetProgress() throws Exception {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNo("PROGRESS_TEST" + System.currentTimeMillis());
        plan.setProductId(1L);
        plan.setProductName("进度测试产品");
        plan.setPlanQuantity(100);
        plan.setCreateBy(1L);

        MvcResult createResult = mockMvc.perform(post("/api/production-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long planId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(get("/api/production-plans/" + planId + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetPlansByStatus() throws Exception {
        mockMvc.perform(get("/api/production-plans/status/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetPlansByOrderId() throws Exception {
        mockMvc.perform(get("/api/production-plans/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testUpdateProductionPlan() throws Exception {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNo("PLAN_UPDATE_TEST" + System.currentTimeMillis());
        plan.setProductId(1L);
        plan.setProductName("更新计划产品");
        plan.setPlanQuantity(100);
        plan.setCreateBy(1L);

        MvcResult createResult = mockMvc.perform(post("/api/production-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long planId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(put("/api/production-plans/" + planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planQuantity\":200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.planQuantity").value(200));
    }

    @Test
    public void testDeleteProductionPlan() throws Exception {
        mockMvc.perform(delete("/api/production-plans/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}