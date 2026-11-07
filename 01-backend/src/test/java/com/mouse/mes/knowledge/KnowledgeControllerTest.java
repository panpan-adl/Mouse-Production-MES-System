package com.mouse.mes.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.config.RolePermissions;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class KnowledgeControllerTest {
    KnowledgeService service;MockMvc mvc;LoginResponse user;
    @BeforeEach void setup() {
        service=mock(KnowledgeService.class);mvc=MockMvcBuilders.standaloneSetup(new KnowledgeController(service,new ObjectMapper())).build();
        user=new LoginResponse();user.setId(7L);user.setRoleId(9L);
    }
    @Test void missingSessionCannotReadDocuments() throws Exception {
        mvc.perform(get("/api/knowledge/documents")).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @Test void ownerIsTakenFromSessionNotRequestBody() throws Exception {
        when(service.upsert(eq(7L),any())).thenReturn(Map.of("unchanged",true));
        mvc.perform(post("/api/knowledge/documents").requestAttr("userInfo",user).contentType("application/json")
                .content("{\"userId\":999,\"owner\":999,\"documentId\":\"doc\",\"title\":\"标题\",\"version\":1,\"text\":\"正文\"}"))
                .andExpect(status().isOk());verify(service).upsert(eq(7L),any());
    }
    @Test void deletionCommitsServiceTombstoneBeforeCleanup() throws Exception {
        when(service.cleanup(7,"doc",2)).thenReturn(Map.of("cleanupPending",false));
        mvc.perform(delete("/api/knowledge/documents/doc?version=2").requestAttr("userInfo",user)).andExpect(status().isOk());
        var order=inOrder(service);order.verify(service).tombstone(7,"doc",2);order.verify(service).cleanup(7,"doc",2);
    }
    @Test void jobPermissionsAllowOnlyAuthenticatedJobRange() {
        for(int role=1;role<=11;role++) assertTrue(RolePermissions.canWrite(role,"/api/knowledge/documents","POST"));
        assertFalse(RolePermissions.canWrite(0,"/api/knowledge/documents","POST"));
        assertFalse(RolePermissions.canWrite(12,"/api/knowledge/documents","POST"));
    }
}
