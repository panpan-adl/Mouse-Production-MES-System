package com.mouse.mes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.entity.ProductionTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductionTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllProductionTasks() throws Exception {
        mockMvc.perform(get("/api/production-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetProductionTaskById() throws Exception {
        mockMvc.perform(get("/api/production-tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetProductionTaskByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/production-tasks/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    public void testCreateProductionTask() throws Exception {
        ProductionTask task = new ProductionTask();
        task.setTaskNo("TASK_TEST" + System.currentTimeMillis());
        task.setPlanId(1L);
        task.setTaskType(1);
        task.setProductId(1L);
        task.setProductName("测试产品");
        task.setQuantity(100);
        task.setTaskStatus(0);

        mockMvc.perform(post("/api/production-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskNo").value(task.getTaskNo()))
                .andExpect(jsonPath("$.data.planId").value(1));
    }

    @Test
    public void testCreateTaskMissingTaskNo() throws Exception {
        ProductionTask task = new ProductionTask();
        task.setPlanId(1L);

        mockMvc.perform(post("/api/production-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    public void testGetTasksByPlanId() throws Exception {
        mockMvc.perform(get("/api/production-tasks/plan/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetTaskByTaskNo() throws Exception {
        ProductionTask task = new ProductionTask();
        task.setTaskNo("TASK_QUERY_TEST" + System.currentTimeMillis());
        task.setPlanId(1L);
        task.setTaskType(1);
        task.setProductId(1L);
        task.setProductName("测试产品");
        task.setQuantity(100);
        task.setTaskStatus(0);

        org.springframework.test.web.servlet.MvcResult createResult = mockMvc.perform(post("/api/production-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String taskNo = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("taskNo").asText();

        mockMvc.perform(get("/api/production-tasks/taskno/" + taskNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskNo").value(taskNo));
    }

    @Test
    public void testUpdateProductionTask() throws Exception {
        ProductionTask task = new ProductionTask();
        task.setTaskNo("TASK_UPDATE_TEST" + System.currentTimeMillis());
        task.setPlanId(1L);
        task.setTaskType(1);
        task.setProductId(1L);
        task.setProductName("测试产品");
        task.setQuantity(100);
        task.setTaskStatus(0);

        org.springframework.test.web.servlet.MvcResult createResult = mockMvc.perform(post("/api/production-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(put("/api/production-tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskStatus\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskStatus").value(1));
    }

    @Test
    public void testDeleteProductionTask() throws Exception {
        mockMvc.perform(delete("/api/production-tasks/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}