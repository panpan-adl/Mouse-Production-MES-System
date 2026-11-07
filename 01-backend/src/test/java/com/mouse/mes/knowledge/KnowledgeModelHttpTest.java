package com.mouse.mes.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeModelHttpTest {
    HttpServer server;ObjectMapper json=new ObjectMapper();KnowledgeModel model;
    String content="OK",reason="stop",receivedPath,authorization;int status=200;
    @BeforeEach void start() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            receivedPath=exchange.getRequestURI().getPath();authorization=exchange.getRequestHeaders().getFirst("Authorization");
            exchange.getRequestBody().readAllBytes();
            byte[] bytes=json.writeValueAsBytes(Map.of("choices",List.of(Map.of("finish_reason",reason,"message",Map.of("content",content)))));
            exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();KnowledgeSettings settings=new KnowledgeSettings();settings.setModel("protocol-fixture");settings.setModelApiKey("fixture-key");
        settings.setModelBaseUrl("http://127.0.0.1:"+server.getAddress().getPort()+"/v1/");model=new KnowledgeModel(settings,json);
    }
    @AfterEach void stop(){server.stop(0);}
    @Test void modelProbeMakesRealHttpCallAndValidatesReply(){model.probe();assertEquals("/v1/chat/completions",receivedPath);assertEquals("Bearer fixture-key",authorization);}
    @Test void probeRejectsUnexpectedReply(){content="无法回答";assertThrows(IllegalStateException.class,model::probe);}
    @Test void non200AndTruncatedOutputAreRejected(){status=401;assertThrows(IllegalStateException.class,model::probe);status=200;reason="length";assertThrows(IllegalStateException.class,model::probe);}
    @Test void markdownWrappedExtractionIsParsedAndDeduplicated() throws Exception {
        var t=new KnowledgeTypes.Triple("点胶工序","使用","点胶机","点胶工序使用点胶机。");
        content="```json\n"+json.writeValueAsString(Map.of("triples",List.of(t,t)))+"\n```";
        assertEquals(List.of(t),model.extract("点胶工序使用点胶机。"));
    }
    @Test void extractionRejectsNonJsonAndUngroundedEntities(){content="不是JSON";assertThrows(IllegalStateException.class,()->model.extract("正文"));content="{\"triples\":[{\"source\":\"假实体\",\"relation\":\"使用\",\"target\":\"假设备\",\"quote\":\"虚构\"}]}";assertThrows(IllegalStateException.class,()->model.extract("正文"));}
    @Test void arkExtractionDisablesThinkingAndRequestsStructuredJson() {
        var body=KnowledgeModel.requestBody(URI.create("https://ark.cn-beijing.volces.com/api/v3/chat/completions"),"doubao-seed-2-1-lite-260915","JSON抽取","资料",true,4096);
        assertEquals(Map.of("type","disabled"),body.get("thinking"));assertEquals(Map.of("type","json_object"),body.get("response_format"));
        assertEquals(false,body.get("stream"));assertEquals(4096,body.get("max_tokens"));
    }
    @Test void arkAnswersUsePlainTextAndGenericModelsKeepTheirOwnProtocol() {
        var answer=KnowledgeModel.requestBody(URI.create("https://ark.cn-beijing.volces.com/api/v3/chat/completions"),"doubao-seed-2-1-lite-260915","回答","问题",false,32);
        assertFalse(answer.containsKey("response_format"));assertEquals(32,answer.get("max_tokens"));
        var generic=KnowledgeModel.requestBody(URI.create("http://127.0.0.1:8081/chat/completions"),"local","JSON抽取","资料",true,4096);
        assertFalse(generic.containsKey("thinking"));assertFalse(generic.containsKey("response_format"));
    }
    @Test void entityLinkerOnlyAcceptsAuthorizedCandidates() {
        content="{\"entities\":[\"点胶机\"]}";assertEquals(List.of("点胶机"),model.linkEntities("点胶设备",List.of("点胶机","设备管理员")));
        content="{\"entities\":[\"别人的设备\"]}";assertThrows(IllegalStateException.class,()->model.linkEntities("设备",List.of("点胶机")));
    }
    @Test void entityLinkerRejectsMalformedResultsAndSupportsNoMatch() {
        content="{\"entities\":[]}";assertTrue(model.linkEntities("未知物料",List.of("点胶机")).isEmpty());
        content="{\"entities\":\"点胶机\"}";assertThrows(IllegalStateException.class,()->model.linkEntities("设备",List.of("点胶机")));
        content="{\"entities\":[\"点胶机\",\"点胶机\"]}";assertEquals(1,model.linkEntities("设备",List.of("点胶机")).size());
    }
}
