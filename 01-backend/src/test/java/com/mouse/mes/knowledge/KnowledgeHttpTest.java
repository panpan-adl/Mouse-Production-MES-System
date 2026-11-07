package com.mouse.mes.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP round trips to a local protocol double, not a claim of live Neo4j/model acceptance. */
class KnowledgeHttpTest {
    HttpServer server;KnowledgeSettings settings;ObjectMapper json=new ObjectMapper();
    AtomicReference<String> body=new AtomicReference<>(), reply=new AtomicReference<>(), path=new AtomicReference<>();
    @BeforeEach void start() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            path.set(exchange.getRequestURI().getPath());body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] bytes=reply.get().getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(202,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();settings=new KnowledgeSettings();settings.setNeo4jPassword("protocol-test-only");settings.setNeo4jUrl("http://127.0.0.1:"+server.getAddress().getPort());
    }
    @AfterEach void stop() { server.stop(0); }
    @Test void queryApiUsesParametersAndRejectsErrorsEvenOn202() throws Exception {
        reply.set("{\"data\":{\"fields\":[\"value\"],\"values\":[[1]]}}");
        var store=new Neo4jKnowledgeStore(settings,json);assertTrue(store.ping());assertEquals("/db/neo4j/query/v2",path.get());
        reply.set("{\"errors\":[{\"code\":\"Neo.ClientError.Statement.SyntaxError\"}]}");
        assertThrows(IllegalStateException.class,store::ping);
    }
    @Test void graphQueryNeverInterpolatesQuestionAndScopesEveryHop() throws Exception {
        reply.set("{\"data\":{\"fields\":[],\"values\":[[\"点胶工序\",\"使用\",\"点胶机\",\"点胶工序使用点胶机\",\"chunk1\"]]}}");
        var result=new Neo4jKnowledgeStore(settings,json).graph(42,List.of("build1"),"' MATCH (x) DETACH DELETE x //");
        var sent=json.readTree(body.get());assertFalse(sent.path("statement").asText().contains("DETACH DELETE"));
        assertTrue(sent.path("statement").asText().contains("all(edge IN relationships(p) WHERE edge.owner=$owner AND edge.buildId IN $builds)"));
        assertEquals(42,sent.path("parameters").path("owner").asInt());assertEquals("build1",sent.path("parameters").path("builds").get(0).asText());assertEquals(1,result.facts().size());
    }
    @Test void deleteIsVersionBoundedToProtectConcurrentNewPublish() throws Exception {
        reply.set("{\"data\":{\"fields\":[],\"values\":[]}}");
        // Capture first request (relation delete) by querying directly, then exercise public operation.
        var store=new Neo4jKnowledgeStore(settings,json);
        List<String> statements=new ArrayList<>();
        server.removeContext("/");server.createContext("/",exchange->{
            try { var sent=json.readTree(exchange.getRequestBody());statements.add(sent.path("statement").asText());assertEquals(42,sent.path("parameters").path("owner").asInt()); }
            catch(Exception e) { throw new RuntimeException(e); }
            byte[] bytes=reply.get().getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(202,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });
        store.delete(42,"doc",2);assertTrue(statements.get(0).contains("r.version<=$version"));assertTrue(statements.get(1).contains("e.owner=$owner"));
    }
}
