package com.mouse.mes.knowledge.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class RagModelClientTest {
    HttpServer server;
    RagModelClient client;
    JdbcTemplate db;
    ObjectMapper json=new ObjectMapper();
    List<Map<String,Object>> requests=new ArrayList<>();
    int status=200;
    String body;
    @BeforeEach void setup() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1");
        db=new JdbcTemplate(source);new RagSchema(db).initialize();
        client=new RagModelClient(json,db,new DataSourceTransactionManager(source));
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            try {
                requests.add(json.readValue(exchange.getRequestBody(),Map.class));
                byte[] response=body.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,response.length);exchange.getResponseBody().write(response);
            } finally {exchange.close();}
        });server.start();
        ReflectionTestUtils.setField(client,"base","http://127.0.0.1:"+server.getAddress().getPort());
        ReflectionTestUtils.setField(client,"key","local-test-secret");ReflectionTestUtils.setField(client,"embedKey","");ReflectionTestUtils.setField(client,"embedBase","");
        ReflectionTestUtils.setField(client,"model","test-chat");ReflectionTestUtils.setField(client,"embedModel","doubao-embedding-vision-251215");ReflectionTestUtils.setField(client,"totalLimit",100);
    }
    @AfterEach void stop(){server.stop(0);}
    @Test void multimodalCreatesOneIndependentVectorPerText() throws Exception {
        double[] vector=new double[1024];Arrays.fill(vector,0.1);
        body=json.writeValueAsString(Map.of("data",Map.of("embedding",vector)));
        var result=client.embed(1,List.of("文档A","文档B"));
        assertEquals(2,requests.size());assertEquals(2,result.size());assertEquals(1024,result.get(0).length);
        assertTrue(json.writeValueAsString(requests.get(0)).contains("文档A"));
        assertFalse(json.writeValueAsString(requests.get(0)).contains("文档B"));
        assertEquals(1024,requests.get(0).get("dimensions"));
    }
    @Test void upstreamFailureDoesNotLeakBodyOrAutomaticallyRetry() {
        status=403;body="local-test-secret and private upstream details";
        var error=assertThrows(IllegalStateException.class,()->client.embed(1,List.of("资料")));
        assertFalse(error.getMessage().contains("secret"));assertEquals(1,requests.size());
    }
    @Test void dimensionMismatchAndInvalidReferencesAreRejected() throws Exception {
        body=json.writeValueAsString(Map.of("data",Map.of("embedding",List.of(1,2))));
        assertThrows(IllegalStateException.class,()->client.embed(1,List.of("资料")));
        body=json.writeValueAsString(Map.of("choices",List.of(Map.of("finish_reason","stop","message",Map.of("content","答案[S2]")))));
        var source=new RagTypes.Source("S1","doc","标题",1,"build:0","资料","正文",0.9);
        assertThrows(IllegalStateException.class,()->client.answer(1,"问题",List.of(source)));
    }
    @Test void lifetimeBudgetRejectsBeforeSending() throws Exception {
        ReflectionTestUtils.setField(client,"totalLimit",1);
        double[] vector=new double[1024];vector[0]=1;
        body=json.writeValueAsString(Map.of("data",Map.of("embedding",vector)));
        client.embed(1,List.of("资料"));
        assertThrows(IllegalStateException.class,()->client.embed(2,List.of("另一个用户")));
        assertEquals(1,requests.size());
    }
    @Test void unifiedReservationSurvivesPublicationRollbackAndHonorsConfiguredDailyLimit(){
        ReflectionTestUtils.setField(client,"totalLimit",100);ReflectionTestUtils.setField(client,"dailyLimit",2);
        var outer=new org.springframework.transaction.support.TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
        outer.executeWithoutResult(tx->{client.reserveGraph(7);tx.setRollbackOnly();});
        assertEquals(1,db.queryForObject("SELECT calls FROM rag_usage WHERE owner=7 AND day=CURRENT_DATE",Integer.class));
        client.reserveGraph(7);assertThrows(IllegalStateException.class,()->client.reserveGraph(7));
        assertEquals(2,db.queryForObject("SELECT calls FROM rag_usage WHERE owner=0 AND day='2000-01-01'",Integer.class));assertTrue(requests.isEmpty());
    }
}
