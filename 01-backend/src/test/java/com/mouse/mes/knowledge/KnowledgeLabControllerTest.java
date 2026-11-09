package com.mouse.mes.knowledge;

import com.mouse.mes.dto.LoginResponse;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class KnowledgeLabControllerTest {
    KnowledgeLabService service;MockMvc mvc;LoginResponse user;
    @BeforeEach void setup(){service=mock(KnowledgeLabService.class);mvc=MockMvcBuilders.standaloneSetup(new KnowledgeLabController(service)).build();user=new LoginResponse();user.setId(7L);}
    @Test void unauthenticatedAndClientSuppliedQueryOwnerAreRejected()throws Exception{
        mvc.perform(get("/api/knowledge/lab/quality")).andExpect(status().isForbidden());
        mvc.perform(get("/api/knowledge/lab/quality?owner=8").requestAttr("userInfo",user)).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @Test void bodyCannotOverrideSessionOwner()throws Exception{
        mvc.perform(post("/api/knowledge/lab/review").requestAttr("userInfo",user).contentType("application/json").content("{\"owner\":8,\"name\":\"DG01\",\"type\":\"EQUIPMENT\"}")).andExpect(status().isOk());
        verify(service).review(7,new KnowledgeLabService.Review("DG01","EQUIPMENT",null));
    }
    @Test void staleConversationAndForeignHistoryHaveDistinctErrors()throws Exception{
        when(service.diff(7,"doc",1,2)).thenThrow(new SecurityException("无权访问"));
        mvc.perform(get("/api/knowledge/lab/diff/doc?before=1&after=2").requestAttr("userInfo",user)).andExpect(status().isForbidden());
        when(service.dialogue(eq(7L),any())).thenThrow(new IllegalStateException("追问资料版本已变化"));
        mvc.perform(post("/api/knowledge/lab/dialogue").requestAttr("userInfo",user).contentType("application/json").content("{\"question\":\"它呢？\",\"contextId\":\"context\"}")).andExpect(status().isServiceUnavailable());
    }
}
