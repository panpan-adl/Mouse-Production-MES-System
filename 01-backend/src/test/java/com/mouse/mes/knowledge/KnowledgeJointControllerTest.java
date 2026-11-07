package com.mouse.mes.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.knowledge.rag.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class KnowledgeJointControllerTest {
    KnowledgeService knowledge; RagFileService files; MockMvc graphMvc,ragMvc; LoginResponse user;
    @BeforeEach void setup(){
        knowledge=mock(KnowledgeService.class);files=mock(RagFileService.class);
        var graph=new KnowledgeController(knowledge,new ObjectMapper());ReflectionTestUtils.setField(graph,"library",files);
        var rag=new RagController(files,mock(RagVectorService.class),mock(RagModelClient.class));ReflectionTestUtils.setField(rag,"knowledge",knowledge);
        graphMvc=MockMvcBuilders.standaloneSetup(graph).build();ragMvc=MockMvcBuilders.standaloneSetup(rag).build();
        user=new LoginResponse();user.setId(7L);user.setRoleId(9L);
    }
    @Test void fileManagedDocumentCannotBeChangedOrDeletedThroughGraphOnlyApi() throws Exception {
        when(files.managed(7,"file-doc")).thenReturn(true);
        graphMvc.perform(post("/api/knowledge/documents").requestAttr("userInfo",user).contentType("application/json")
            .content("{\"documentId\":\"file-doc\",\"title\":\"新标题\",\"version\":2,\"text\":\"新正文\"}"))
            .andExpect(status().isBadRequest());
        graphMvc.perform(delete("/api/knowledge/documents/file-doc?version=2").requestAttr("userInfo",user))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(knowledge);
    }
    @Test void ragEndpointForwardsAllModesAndScopeUsingSessionOwner() throws Exception {
        when(files.graphIntegrated()).thenReturn(true);
        for(String mode:List.of("RAG","GRAPHRAG","HYBRID")){
            ragMvc.perform(post("/api/rag/messages").requestAttr("userInfo",user).contentType("application/json")
                .content("{\"question\":\"谁负责设备？\",\"mode\":\""+mode+"\",\"documentIds\":[\"file-doc\"]}"))
                .andExpect(status().isOk());
            verify(knowledge).ask(7,new KnowledgeTypes.Question("谁负责设备？",mode,List.of("file-doc")));
        }
    }
    @Test void ragEndpointRejectsClientSuppliedOwnerAndMalformedScope() throws Exception {
        when(files.graphIntegrated()).thenReturn(true);
        ragMvc.perform(post("/api/rag/messages").requestAttr("userInfo",user).contentType("application/json")
            .content("{\"question\":\"设备\",\"owner\":999}" )).andExpect(status().isBadRequest());
        ragMvc.perform(post("/api/rag/messages?owner=999").requestAttr("userInfo",user).contentType("application/json")
            .content("{\"question\":\"设备\"}" )).andExpect(status().isForbidden());
        ragMvc.perform(post("/api/rag/messages").requestAttr("userInfo",user).contentType("application/json")
            .content("{\"question\":\"设备\",\"documentIds\":[999]}" )).andExpect(status().isBadRequest());
        verifyNoInteractions(knowledge);
    }
}
