package com.mouse.mes.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductionDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orders").exists())
                .andExpect(jsonPath("$.data.plans").exists())
                .andExpect(jsonPath("$.data.workOrders").exists())
                .andExpect(jsonPath("$.data.totalTasks").exists());
    }

    @Test
    public void testGetOrdersSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orders.pending").exists())
                .andExpect(jsonPath("$.data.orders.confirmed").exists())
                .andExpect(jsonPath("$.data.orders.producing").exists())
                .andExpect(jsonPath("$.data.orders.completed").exists());
    }

    @Test
    public void testGetPlansSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.plans.pending").exists())
                .andExpect(jsonPath("$.data.plans.inProgress").exists())
                .andExpect(jsonPath("$.data.plans.completed").exists())
                .andExpect(jsonPath("$.data.plans.totalQuantity").exists())
                .andExpect(jsonPath("$.data.plans.completedQuantity").exists())
                .andExpect(jsonPath("$.data.plans.completionRate").exists());
    }

    @Test
    public void testGetWorkOrdersSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.workOrders.planned").exists())
                .andExpect(jsonPath("$.data.workOrders.assigned").exists())
                .andExpect(jsonPath("$.data.workOrders.inProgress").exists())
                .andExpect(jsonPath("$.data.workOrders.completed").exists());
    }

    @Test
    public void testGetRecentOrders() throws Exception {
        mockMvc.perform(get("/api/dashboard/recent-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetRecentPlans() throws Exception {
        mockMvc.perform(get("/api/dashboard/recent-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetRecentWorkOrders() throws Exception {
        mockMvc.perform(get("/api/dashboard/recent-work-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}