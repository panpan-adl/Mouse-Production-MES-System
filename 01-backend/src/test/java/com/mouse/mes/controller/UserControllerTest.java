package com.mouse.mes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.entity.User;
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
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetUserById() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCode").value("109001"));
    }

    @Test
    public void testGetUserByUserCode() throws Exception {
        mockMvc.perform(get("/api/users/usercode/109001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCode").value("109001"));
    }

    @Test
    public void testGetUsersByRoleCode() throws Exception {
        mockMvc.perform(get("/api/users/role/WORKSHOP_MANAGER"))    
                .andExpect(status().isOk());
    }

    @Test
    public void testGetUsersByDeptId() throws Exception {
        mockMvc.perform(get("/api/users/dept/1"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetAllUsersIncludingDeleted() throws Exception {
        mockMvc.perform(get("/api/users/all"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetDeletedUsers() throws Exception {
        mockMvc.perform(get("/api/users/deleted"))
                .andExpect(status().isOk());
    }

    @Test
    public void testEmployeeLeave() throws Exception {
        mockMvc.perform(post("/api/users/999/leave"))
                .andExpect(status().isOk());
    }

    @Test
    public void testRestoreUser() throws Exception {
        mockMvc.perform(post("/api/users/999/restore"))
                .andExpect(status().isOk());
    }
}