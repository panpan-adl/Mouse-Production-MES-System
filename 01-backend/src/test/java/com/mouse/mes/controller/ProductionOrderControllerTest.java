package com.mouse.mes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.entity.ProductionOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductionOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllProductionOrders() throws Exception {
        mockMvc.perform(get("/api/production-order/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetProductionOrderById() throws Exception {
        mockMvc.perform(get("/api/production-order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetProductionOrderByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/production-order/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    public void testCreateProductionOrder() throws Exception {
        ProductionOrder po = new ProductionOrder();
        po.setPoNo("PO_TEST" + System.currentTimeMillis());
        po.setProductId(1L);
        po.setPlannedQuantity(new BigDecimal("100"));
        po.setStatus("PLANNED");
        po.setCreatedBy(1L);

        mockMvc.perform(post("/api/production-order/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(po)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.poNo").value(po.getPoNo()))
                .andExpect(jsonPath("$.data.status").value("PLANNED"));
    }

    @Test
    public void testUpdateProductionOrder() throws Exception {
        mockMvc.perform(put("/api/production-order/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poNo\":\"UPDATED_PO\",\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testDeleteProductionOrder() throws Exception {
        mockMvc.perform(delete("/api/production-order/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    public void testGetProductionOrderByNo() throws Exception {
        ProductionOrder po = new ProductionOrder();
        po.setPoNo("PO_QUERY_TEST" + System.currentTimeMillis());
        po.setProductId(1L);
        po.setPlannedQuantity(new BigDecimal("100"));
        po.setStatus("PLANNED");
        po.setCreatedBy(1L);

        org.springframework.test.web.servlet.MvcResult createResult = mockMvc.perform(post("/api/production-order/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(po)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String poNo = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("poNo").asText();

        mockMvc.perform(get("/api/production-order/no/" + poNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.poNo").value(poNo));
    }
}