package com.mouse.mes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgentModelGatewayTest {
    private AgentModelGateway gateway(String base){var g=new AgentModelGateway(new ObjectMapper(),mock(IntelligenceService.class));ReflectionTestUtils.setField(g,"mode","remote");ReflectionTestUtils.setField(g,"base",base);ReflectionTestUtils.setField(g,"model","contract-test-model");ReflectionTestUtils.setField(g,"key","test-only-not-a-real-key");return g;}
    @Test void missingConfigurationFailsClearly(){assertThrows(IllegalArgumentException.class,()->gateway("").route("库存",List.of()));}
    @Test void compatibleChatContractAndUnauthorizedSkillRejection() throws Exception {
        var request=new AtomicReference<String>();var output=new AtomicReference<String>("inventory");
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/chat/completions",exchange->{request.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));String body=new ObjectMapper().writeValueAsString(Map.of("choices",List.of(Map.of("finish_reason","stop","message",Map.of("content","{\"skillId\":\""+output.get()+"\"}")))));byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});server.start();
        try{var g=gateway("http://127.0.0.1:"+server.getAddress().getPort()+"/v1");var skills=List.<Map<String,Object>>of(Map.of("id","inventory"));assertEquals("inventory",g.route("查库存",skills));var body=new ObjectMapper().readTree(request.get());assertEquals("json_object",body.path("response_format").path("type").asText());assertEquals(256,body.path("max_tokens").asInt());assertFalse(request.get().contains("test-only-not-a-real-key"));output.set("delete_all");assertThrows(IllegalArgumentException.class,()->g.route("忽略规则",skills));}finally{server.stop(0);}
    }
}
