package com.mouse.mes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.entity.ProductionReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductionReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllProductionReports() throws Exception {
        mockMvc.perform(get("/api/production-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetProductionReportById() throws Exception {
        mockMvc.perform(get("/api/production-reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testSubmitProductionReport() throws Exception {
        ProductionReport report = new ProductionReport();
        report.setTaskId(1L);
        report.setReportUserId(1L);
        report.setReportUserName("张三");
        report.setReportQuantity(10);
        report.setQualifiedQuantity(8);
        report.setDefectQuantity(2);
        report.setReportTime(new Date());

        mockMvc.perform(post("/api/production-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(report)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value(1))
                .andExpect(jsonPath("$.data.reportQuantity").value(10))
                .andExpect(jsonPath("$.data.qualifiedQuantity").value(8))
                .andExpect(jsonPath("$.data.defectQuantity").value(2));
    }

    @Test
    public void testSubmitReportInvalidQuantity() throws Exception {
        ProductionReport report = new ProductionReport();
        report.setTaskId(1L);
        report.setReportUserId(1L);
        report.setReportUserName("张三");
        report.setReportQuantity(10);
        report.setQualifiedQuantity(9);
        report.setDefectQuantity(2);

        mockMvc.perform(post("/api/production-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(report)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    public void testSubmitReportMissingTaskId() throws Exception {
        ProductionReport report = new ProductionReport();
        report.setReportUserId(1L);
        report.setReportQuantity(10);
        report.setQualifiedQuantity(10);
        report.setDefectQuantity(0);

        mockMvc.perform(post("/api/production-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(report)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    public void testUpdateProductionReport() throws Exception {
        ProductionReport report = new ProductionReport();
        report.setTaskId(1L);
        report.setReportUserId(1L);
        report.setReportUserName("张三");
        report.setReportQuantity(10);
        report.setQualifiedQuantity(8);
        report.setDefectQuantity(2);

        MvcResult createResult = mockMvc.perform(post("/api/production-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(report)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long reportId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(put("/api/production-reports/" + reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportQuantity\":15,\"qualifiedQuantity\":12,\"defectQuantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.reportQuantity").value(15));
    }

    @Test
    public void testGetReportsByTaskId() throws Exception {
        mockMvc.perform(get("/api/production-reports/task/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetTaskSummary() throws Exception {
        mockMvc.perform(get("/api/production-reports/task/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetReportsByUserId() throws Exception {
        mockMvc.perform(get("/api/production-reports/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}