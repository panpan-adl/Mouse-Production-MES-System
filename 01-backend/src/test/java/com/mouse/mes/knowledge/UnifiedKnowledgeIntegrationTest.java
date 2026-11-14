package com.mouse.mes.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.knowledge.rag.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real isolated MySQL + real Neo4j; only the cloud-model protocol is a deterministic fixture. */
@EnabledIfEnvironmentVariable(named="MES_UNIFIED_IT",matches="true")
class UnifiedKnowledgeIntegrationTest {
    @TempDir Path storage;
    @Test void atomicDualPublicationTwoHopRetrievalAndOfflineDeletion() throws Exception {
        var json=new ObjectMapper();
        var admin=new JdbcTemplate(new DriverManagerDataSource("jdbc:mysql://127.0.0.1:3306/?useSSL=false&allowPublicKeyRetrieval=true","root",System.getenv("SPRING_DATASOURCE_PASSWORD")));
        String database="mouse_mes_unified_it_"+UUID.randomUUID().toString().replace("-","");
        admin.execute("CREATE DATABASE "+database+" CHARACTER SET utf8mb4");
        var datasource=new DriverManagerDataSource("jdbc:mysql://127.0.0.1:3306/"+database+"?useSSL=false&allowPublicKeyRetrieval=true","root",System.getenv("SPRING_DATASOURCE_PASSWORD"));
        var db=new JdbcTemplate(datasource);var tx=new DataSourceTransactionManager(datasource);
        new KnowledgeSchema(datasource).run();var schema=new RagSchema(db);schema.initialize();
        var settings=new KnowledgeSettings();settings.setEnabled(true);settings.setNeo4jUrl(System.getenv("MES_KNOWLEDGE_NEO4J_URL"));settings.setNeo4jPassword(System.getenv("MES_KNOWLEDGE_NEO4J_PASSWORD"));
        String graphUrl=settings.getNeo4jUrl();var graph=new Neo4jKnowledgeStore(settings,json);assertTrue(graph.ping());
        long owner=1000000L+Math.abs(new Random().nextInt(1000000));String id1="it-a-"+UUID.randomUUID(),id2="it-b-"+UUID.randomUUID();
        var failEmbedding=new java.util.concurrent.atomic.AtomicBoolean();
        HttpServer llm=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        llm.createContext("/",exchange->{try{
            var input=json.readTree(exchange.getRequestBody());Map<String,Object> response;int status=200;
            if(exchange.getRequestURI().getPath().contains("embeddings")) {
                double[] vector=new double[1024];vector[0]=1;response=Map.of("data",Map.of("embedding",vector));if(failEmbedding.get())status=503;
            }else{
                String text=input.path("messages").path(1).path("content").asText();String output="依据资料[S1]。";
                if(input.path("messages").path(0).path("content").asText().contains("抽取器")) {
                    List<KnowledgeTypes.Triple> triples=new ArrayList<>();
                    if(text.contains("点胶工序使用点胶机。"))triples.add(new KnowledgeTypes.Triple("点胶工序","使用","点胶机","点胶工序使用点胶机。"));
                    for(String person:List.of("张师傅","李师傅"))if(text.contains("点胶机负责人是"+person+"。"))triples.add(new KnowledgeTypes.Triple("点胶机","负责人",person,"点胶机负责人是"+person+"。"));
                    output=json.writeValueAsString(Map.of("triples",triples));
                }
                response=Map.of("choices",List.of(Map.of("finish_reason","stop","message",Map.of("content",output))));
            }
            byte[] bytes=json.writeValueAsBytes(response);exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);
        }finally{exchange.close();}});llm.start();
        String endpoint="http://127.0.0.1:"+llm.getAddress().getPort();settings.setModelBaseUrl(endpoint);settings.setModel("fixture");settings.setModelApiKey("fixture");
        var client=new RagModelClient(json,db,tx);
        for(var entry:Map.of("base",endpoint,"model","fixture","key","fixture","embedModel","doubao-embedding-vision-251215","embedBase","","embedKey","").entrySet())ReflectionTestUtils.setField(client,entry.getKey(),entry.getValue());
        ReflectionTestUtils.setField(client,"totalLimit",100);
        var index=new AuthorizedChunkIndex(db,client,json,schema);var retriever=new VectorRagRetriever(index);
        var repository=new KnowledgeRepository(db,json);var service=new KnowledgeService(settings,repository,graph,new KnowledgeModel(settings,json),retriever);
        ReflectionTestUtils.setField(service,"vectorIndex",retriever);
        var factory=new ProxyFactory(service);factory.setProxyTargetClass(true);factory.addAdvice(new TransactionInterceptor(tx,new AnnotationTransactionAttributeSource()));
        var knowledge=(KnowledgeService)factory.getProxy();
        var files=new RagFileService(db,tx,new RagParser(),client,json,schema,storage.toString());
        files.setGraphBridge(new KnowledgeGraphBridge(knowledge,repository,index));
        try {
            assertEquals("READY",files.upload(owner,id1,1,"工艺","a.txt","点胶工序使用点胶机。".getBytes(StandardCharsets.UTF_8)).get("status"));
            assertEquals("READY",files.upload(owner,id2,1,"点检","b.txt","点胶机负责人是张师傅。".getBytes(StandardCharsets.UTF_8)).get("status"));
            var docs=repository.list(owner);assertEquals(2,docs.size());
            for(var doc:docs)for(var chunk:doc.chunks())assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM rag_vector WHERE owner=? AND chunk_id=? AND version=?",Integer.class,owner,chunk.id(),doc.version()));
            for(String mode:List.of("RAG","GRAPHRAG","HYBRID")) {
                var answer=knowledge.ask(owner,new KnowledgeTypes.Question("点胶工序",mode));assertEquals(2,answer.sources().size());
                if(!mode.equals("RAG"))assertTrue(answer.facts().stream().anyMatch(f->f.target().equals("张师傅")));
            }
            assertTrue(knowledge.ask(owner+1,new KnowledgeTypes.Question("点胶工序","HYBRID")).sources().isEmpty());
            assertThrows(SecurityException.class,()->files.download(owner+1,id1));
            // Neo4j fails before publication: v1 remains active in BOTH systems.
            settings.setNeo4jUrl("http://127.0.0.1:9");
            assertEquals("FAILED",files.upload(owner,id2,2,"点检新版","b.txt","点胶机负责人是李师傅。".getBytes(StandardCharsets.UTF_8)).get("status"));
            assertEquals(1,files.detail(owner,id2).get("activeVersion"));settings.setNeo4jUrl(graphUrl);
            // Neo4j succeeds, embedding fails: the MySQL publish transaction still rolls back.
            failEmbedding.set(true);assertEquals("FAILED",files.retry(owner,id2,2).get("status"));failEmbedding.set(false);
            assertEquals(1,repository.list(owner).stream().filter(d->d.documentId().equals(id2)).findFirst().orElseThrow().version());
            assertTrue(knowledge.visualize(owner).facts().stream().anyMatch(f->f.target().equals("张师傅")));
            assertFalse(knowledge.visualize(owner).facts().stream().anyMatch(f->f.target().equals("李师傅")));
            assertEquals("READY",files.retry(owner,id2,2).get("status"));
            assertTrue(knowledge.visualize(owner).facts().stream().anyMatch(f->f.target().equals("李师傅")));
            assertFalse(knowledge.visualize(owner).facts().stream().anyMatch(f->f.target().equals("张师傅")));
            // Offline cleanup cannot restore a deleted file, vector or graph permission.
            settings.setNeo4jUrl("http://127.0.0.1:9");assertEquals(true,files.delete(owner,id2,3).get("graphCleanupPending"));
            assertThrows(SecurityException.class,()->files.download(owner,id2));
            assertFalse(repository.list(owner).stream().anyMatch(d->d.documentId().equals(id2)));
            assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM rag_authorized_vector WHERE owner=? AND document_id=?",Integer.class,owner,id2));
            settings.setNeo4jUrl(graphUrl);assertEquals(false,files.retry(owner,id2,3).get("graphCleanupPending"));
            assertEquals("READY",files.upload(owner,id2,4,"重建","b.txt","点胶机负责人是李师傅。".getBytes(StandardCharsets.UTF_8)).get("status"));
            assertThrows(IllegalArgumentException.class,()->files.delete(owner,id2,3));
            assertTrue(repository.list(owner).stream().anyMatch(d->d.documentId().equals(id2)&&d.version()==4));
            System.out.println("UNIFIED MYSQL+NEO4J ACCEPTANCE PASSED: shared chunks, three modes, two hops, isolation, dual rollback, delete and cleanup retry");
        } finally {
            settings.setNeo4jUrl(graphUrl);graph.delete(owner,id1,Long.MAX_VALUE);graph.delete(owner,id2,Long.MAX_VALUE);llm.stop(0);
            if(!database.matches("mouse_mes_unified_it_[a-f0-9]{32}"))throw new IllegalStateException("Unexpected test database name");
            admin.execute("DROP DATABASE "+database);
        }
    }
}
