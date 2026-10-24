package com.mouse.mes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.entity.WorkOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class WorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllWorkOrders() throws Exception {
        mockMvc.perform(get("/api/work-order/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetWorkOrderById() throws Exception {
        mockMvc.perform(get("/api/work-order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testCreateWorkOrder() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo("WO_TEST" + System.currentTimeMillis());
        workOrder.setProductionOrderId(1L);
        workOrder.setProductId(1L);
        workOrder.setPlannedQuantity(new BigDecimal("100"));
        workOrder.setCreatedBy(1L);

        mockMvc.perform(post("/api/work-order/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.woNo").value(workOrder.getWoNo()))
                .andExpect(jsonPath("$.data.status").value("PLANNED"));
    }

    @Test
    public void testAssignWorkOrder() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo("WO_ASSIGN" + System.currentTimeMillis());
        workOrder.setProductionOrderId(1L);
        workOrder.setProductId(1L);
        workOrder.setPlannedQuantity(new BigDecimal("50"));
        workOrder.setCreatedBy(1L);

        MvcResult createResult = mockMvc.perform(post("/api/work-order/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long woId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(put("/api/work-order/" + woId + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"userName\":\"张三\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.assignedUserId").value(1))
                .andExpect(jsonPath("$.data.assignedUserName").value("张三"));
    }

    @Test
    public void testClaimWorkOrder() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo("WO_CLAIM" + System.currentTimeMillis());
        workOrder.setProductionOrderId(1L);
        workOrder.setProductId(1L);
        workOrder.setPlannedQuantity(new BigDecimal("30"));
        workOrder.setCreatedBy(1L);

        MvcResult createResult = mockMvc.perform(post("/api/work-order/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long woId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(put("/api/work-order/" + woId + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"userName\":\"张三\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(put("/api/work-order/" + woId + "/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    public void testCompleteWorkOrder() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo("WO_COMPLETE" + System.currentTimeMillis());
        workOrder.setProductionOrderId(1L);
        workOrder.setProductId(1L);
        workOrder.setPlannedQuantity(new BigDecimal("20"));
        workOrder.setCreatedBy(1L);

        MvcResult createResult = mockMvc.perform(post("/api/work-order/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long woId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(put("/api/work-order/" + woId + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"userName\":\"张三\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/work-order/" + woId + "/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/work-order/" + woId + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualQuantity\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.actualQuantity").value(20));
    }

    @Test
    public void testGetWorkOrdersByStatus() throws Exception {
        mockMvc.perform(get("/api/work-order/status/PLANNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetWorkOrdersByUser() throws Exception {
        mockMvc.perform(get("/api/work-order/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetWorkOrdersByProductionOrder() throws Exception {
        mockMvc.perform(get("/api/work-order/production/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testUpdateWorkOrder() throws Exception {
        mockMvc.perform(put("/api/work-order/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"woNo\":\"UPDATED_WO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testDeleteWorkOrder() throws Exception {
        mockMvc.perform(delete("/api/work-order/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}