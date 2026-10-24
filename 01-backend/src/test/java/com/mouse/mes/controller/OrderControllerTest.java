package com.mouse.mes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.entity.Order;
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
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllOrders() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetOrderById() throws Exception {
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetOrderByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/orders/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    public void testCreateOrder() throws Exception {
        Order order = new Order();
        order.setOrderNo("TEST" + System.currentTimeMillis());
        order.setCustomerName("测试客户");
        order.setCustomerPhone("13800138000");
        order.setProductName("测试产品");
        order.setQuantity(100);
        order.setUnitPrice(new BigDecimal("10.00"));
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.setOrderDate(new Date());
        order.setDeliveryDate(new Date());
        order.setCreateBy(1L);

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderNo").value(order.getOrderNo()))
                .andExpect(jsonPath("$.data.customerName").value(order.getCustomerName()))
                .andReturn();

        System.out.println("Create order response: " + result.getResponse().getContentAsString());
    }

    @Test
    public void testCreateOrderMissingOrderNo() throws Exception {
        Order order = new Order();
        order.setCustomerName("测试客户");
        order.setQuantity(100);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    public void testConfirmOrder() throws Exception {
        Order order = new Order();
        order.setOrderNo("CONFIRM" + System.currentTimeMillis());
        order.setCustomerName("确认测试客户");
        order.setProductName("测试产品");
        order.setQuantity(50);
        order.setUnitPrice(new BigDecimal("20.00"));
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.setOrderDate(new Date());
        order.setDeliveryDate(new Date());
        order.setCreateBy(1L);

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(response).get("data").get("id").asLong();

        mockMvc.perform(put("/api/orders/" + orderId + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderStatus").value(1));
    }

    @Test
    public void testCancelOrder() throws Exception {
        Order order = new Order();
        order.setOrderNo("CANCEL" + System.currentTimeMillis());
        order.setCustomerName("取消测试客户");
        order.setProductName("测试产品");
        order.setQuantity(30);
        order.setUnitPrice(new BigDecimal("15.00"));
        order.setTotalAmount(new BigDecimal("450.00"));
        order.setOrderDate(new Date());
        order.setDeliveryDate(new Date());
        order.setCreateBy(1L);

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long orderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(put("/api/orders/" + orderId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderStatus").value(4));
    }

    @Test
    public void testGetOrdersByStatus() throws Exception {
        mockMvc.perform(get("/api/orders/status/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testUpdateOrder() throws Exception {
        Order order = new Order();
        order.setOrderNo("UPDATE_TEST" + System.currentTimeMillis());
        order.setCustomerName("更新测试客户");
        order.setProductName("更新产品");
        order.setQuantity(80);
        order.setUnitPrice(new BigDecimal("12.00"));
        order.setTotalAmount(new BigDecimal("960.00"));
        order.setOrderDate(new Date());
        order.setDeliveryDate(new Date());
        order.setCreateBy(1L);

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long orderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        Order updateOrder = new Order();
        updateOrder.setOrderNo("UPDATED_" + System.currentTimeMillis());
        updateOrder.setCustomerName("更新后的客户");
        updateOrder.setProductName("更新产品");
        updateOrder.setQuantity(90);

        mockMvc.perform(put("/api/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderNo").value(updateOrder.getOrderNo()))
                .andExpect(jsonPath("$.data.customerName").value(updateOrder.getCustomerName()));
    }

    @Test
    public void testDeleteOrder() throws Exception {
        mockMvc.perform(delete("/api/orders/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}