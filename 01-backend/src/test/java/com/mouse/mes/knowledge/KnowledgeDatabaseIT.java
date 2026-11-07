package com.mouse.mes.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static com.mouse.mes.knowledge.KnowledgeTypes.*;

/** Opt-in integration against isolated REAL MySQL and Neo4j. LLM is an explicit local HTTP fixture. */
@EnabledIfEnvironmentVariable(named="KNOWLEDGE_IT",matches="true")
class KnowledgeDatabaseIT {
    static KnowledgeService service; static KnowledgeRepository repo; static Neo4jKnowledgeStore graph;
    static JdbcTemplate db; static HttpServer llm; static ObjectMapper json=new ObjectMapper();
    static AtomicInteger extractions=new AtomicInteger();
    long owner; String prefix;
    @BeforeAll static void configure() throws Exception {
        String url=System.getenv("KNOWLEDGE_IT_MYSQL_URL");
        if(url==null || !url.matches("jdbc:mysql://127\\.0\\.0\\.1:13316/mouse_mes_kg_it[?].*"))
            throw new IllegalStateException("Integration tests require dedicated localhost:13316/mouse_mes_kg_it database");
        var dataSource=new DriverManagerDataSource(url,"root",System.getenv().getOrDefault("KNOWLEDGE_IT_MYSQL_PASSWORD",""));
        db=new JdbcTemplate(dataSource);new KnowledgeSchema(dataSource).run();
        llm=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        llm.createContext("/chat/completions",exchange->{
            try {
                var request=json.readTree(exchange.getRequestBody());String system=request.path("messages").path(0).path("content").asText();
                String text=request.path("messages").path(1).path("content").asText();String content;
                if(system.contains("抽取器")) {
                    extractions.incrementAndGet();List<Triple> triples=new ArrayList<>();
                    if(text.contains("FAIL")) triples.add(new Triple("幻觉实体","错误","幻觉目标","伪造引用"));
                    else if(text.contains("备用点胶机")) triples.add(new Triple("点胶工序","使用","备用点胶机","点胶工序使用备用点胶机。"));
                    else {
                        if(text.contains("点胶工序使用点胶机")) triples.add(new Triple("点胶工序","使用","点胶机","点胶工序使用点胶机。"));
                        if(text.contains("点胶机由设备管理员负责")) triples.add(new Triple("点胶机","负责人","设备管理员","点胶机由设备管理员负责。"));
                    }
                    content=json.writeValueAsString(Map.of("triples",triples));
                } else if(system.contains("连接测试")) content="OK";
                else content="已根据提供的资料检索到相关信息。[S1]";
                byte[] bytes=json.writeValueAsBytes(Map.of("choices",List.of(Map.of("finish_reason","stop","message",Map.of("content",content)))));
                exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);
            } finally { exchange.close(); }
        });llm.start();
        KnowledgeSettings settings=new KnowledgeSettings();settings.setEnabled(true);
        settings.setNeo4jUrl("http://127.0.0.1:17474");settings.setNeo4jPassword(System.getenv("KNOWLEDGE_IT_NEO4J_PASSWORD"));
        settings.setModelBaseUrl("http://127.0.0.1:"+llm.getAddress().getPort());settings.setModel("integration-http-fixture");settings.setModelApiKey("local-fixture");
        repo=new KnowledgeRepository(db,json);graph=new Neo4jKnowledgeStore(settings,json);graph.ping();
        KnowledgeService target=new KnowledgeService(settings,repo,graph,new KnowledgeModel(settings,json),new LexicalRagRetriever());
        ProxyFactory factory=new ProxyFactory(target);factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),new AnnotationTransactionAttributeSource()));
        service=(KnowledgeService)factory.getProxy();
    }
    @BeforeEach void identity() { owner=Math.abs(UUID.randomUUID().getMostSignificantBits()%1000000000000L)+1000;prefix=UUID.randomUUID().toString(); }
    @AfterEach void removeFixtures() {
        for(Document d:repo.list(owner)) { service.tombstone(owner,d.documentId(),d.version()+1);service.cleanup(owner,d.documentId(),d.version()+1); }
    }
    @AfterAll static void shutdown() { if(llm!=null)llm.stop(0); }
    DocumentInput input(String suffix,long version,String text) { return new DocumentInput(prefix+suffix,"集成测试资料",version,text); }

    @Test void realCrossDocumentTwoHopQueryUsesOnlyAuthorizedBuilds() {
        service.upsert(owner,input("-sop",1,"点胶工序使用点胶机。"));
        service.upsert(owner,input("-equipment",1,"点胶机由设备管理员负责。"));
        var docs=repo.list(owner);assertEquals(2,docs.size());
        Graph result=graph.graph(owner,docs.stream().map(Document::buildId).toList(),"点胶工序使用的设备由谁负责？");
        assertEquals(2,result.facts().size());assertTrue(result.facts().stream().anyMatch(f->f.target().equals("设备管理员")));
        assertTrue(graph.graph(owner+1,docs.stream().map(Document::buildId).toList(),"").facts().isEmpty());
        assertTrue(graph.entities(owner,docs.stream().map(Document::buildId).toList()).contains("点胶机"));
        assertTrue(graph.entities(owner+1,docs.stream().map(Document::buildId).toList()).isEmpty());
        Graph linked=graph.graph(owner,docs.stream().map(Document::buildId).toList(),"胶合这道工艺的机器归谁管",List.of("点胶工序"));
        assertEquals(2,linked.facts().size());
        assertThrows(SecurityException.class,()->service.ask(owner+1,new Question("点胶工序","GRAPHRAG",List.of(prefix+"-sop"))));
        Answer answer=service.ask(owner,new Question("点胶工序使用的设备由谁负责？","GRAPHRAG"));
        assertEquals(2,answer.sources().size());assertEquals(2,answer.facts().size());assertEquals("integration-http-fixture",answer.model());
        service.probeModel();
    }
    @Test void realVersionSwitchAndDeletionExcludeStaleRelations() {
        service.upsert(owner,input("-sop",1,"点胶工序使用点胶机。"));String old=repo.list(owner).get(0).buildId();
        service.upsert(owner,input("-sop",2,"点胶工序使用备用点胶机。"));
        assertNotEquals(old,repo.list(owner).get(0).buildId());
        Graph current=service.visualize(owner);assertEquals(1,current.facts().size());assertEquals("备用点胶机",current.facts().get(0).target());
        service.tombstone(owner,prefix+"-sop",3);assertTrue(service.documents(owner).isEmpty());assertEquals(1,service.pendingDeletions(owner).size());
        service.upsert(owner,input("-sop",4,"点胶工序使用点胶机。"));
        service.cleanup(owner,prefix+"-sop",3);assertEquals(1,service.visualize(owner).facts().size());
        assertTrue(graph.graph(owner,List.of(old),"").facts().isEmpty());
        assertEquals(4,repo.list(owner).get(0).version());assertTrue(service.pendingDeletions(owner).isEmpty());
    }
    @Test void actualMysqlTransactionRollsBackPlaceholderAfterExtractionFailure() {
        assertThrows(IllegalStateException.class,()->service.upsert(owner,input("-bad",1,"FAIL")));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM mm_kg_document WHERE owner_id=?",Integer.class,owner));
    }
    @Test void concurrentSameVersionRequestsBuildExactlyOnce() throws Exception {
        int before=extractions.get();var input=input("-race",1,"点胶工序使用点胶机。");
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<Map<String,Object>> a=pool.submit(()->service.upsert(owner,input));Future<Map<String,Object>> b=pool.submit(()->service.upsert(owner,input));
            var results=List.of(a.get(40,TimeUnit.SECONDS),b.get(40,TimeUnit.SECONDS));
            assertEquals(1,results.stream().filter(r->Boolean.TRUE.equals(r.get("unchanged"))).count());
            assertEquals(before+1,extractions.get());assertEquals(1,service.visualize(owner).facts().size());
        } finally { pool.shutdownNow(); }
    }
}
