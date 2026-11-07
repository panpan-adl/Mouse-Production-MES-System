package com.mouse.mes.knowledge.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in: actual MySQL + actual paid Ark embedding/chat. No Redis/login claim. */
@EnabledIfEnvironmentVariable(named="MES_RAG_LIVE",matches="true")
class RagRealProviderTest {
    @TempDir Path storage;
    @Test void realEmbeddingRetrievalReplacementAndDeletion() throws Exception {
        var datasource=new DriverManagerDataSource("jdbc:mysql://127.0.0.1:3306/mouse_mes?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai","root",System.getenv("SPRING_DATASOURCE_PASSWORD"));
        var db=new JdbcTemplate(datasource);var tx=new DataSourceTransactionManager(datasource);
        var schema=new RagSchema(db);schema.initialize();
        var json=new ObjectMapper();var client=new RagModelClient(json,db,tx);
        ReflectionTestUtils.setField(client,"base","https://ark.cn-beijing.volces.com/api/v3");
        ReflectionTestUtils.setField(client,"model","doubao-seed-2-1-lite-260915");
        ReflectionTestUtils.setField(client,"key",System.getenv("MES_AI_API_KEY"));
        ReflectionTestUtils.setField(client,"embedBase","");ReflectionTestUtils.setField(client,"embedKey","");
        ReflectionTestUtils.setField(client,"embedModel","doubao-embedding-vision-251215");ReflectionTestUtils.setField(client,"totalLimit",100);
        var files=new RagFileService(db,tx,new RagParser(),client,json,schema,storage.toString());
        var vectors=new RagVectorService(db,client,json);ReflectionTestUtils.setField(vectors,"minimumScore",0.5);
        long owner=db.queryForObject("SELECT id FROM t_user WHERE user_code='109001'",Long.class);
        String id="live-"+UUID.randomUUID();
        try {
            byte[] original="A100鼠标点胶工序使用点胶机。点胶机由张师傅负责，每天开工前检查胶量。".getBytes(StandardCharsets.UTF_8);
            assertEquals("READY",files.upload(owner,id,1,"联调工艺规范","sop.txt",original).get("status"));
            assertArrayEquals(original,files.download(owner,id).bytes());
            var reply=vectors.ask(owner,"点胶机由谁负责？","RAG");
            assertTrue(reply.answer().contains("张师傅"),reply.answer());
            assertTrue(reply.sources().stream().anyMatch(s->s.documentId().equals(id)&&s.version()==1));
            assertTrue(vectors.ask(99999999,"点胶机由谁负责？","RAG").sources().isEmpty());
            assertThrows(SecurityException.class,()->files.download(99999999,id));
            assertEquals("READY",files.upload(owner,id,2,"联调工艺规范","sop.txt","A100鼠标点胶机负责人已变更为李师傅。每天开工前检查胶量。".getBytes(StandardCharsets.UTF_8)).get("status"));
            var updated=vectors.ask(owner,"点胶机由谁负责？","RAG");
            assertTrue(updated.answer().contains("李师傅"),updated.answer());
            assertTrue(updated.sources().stream().filter(s->s.documentId().equals(id)).allMatch(s->s.version()==2));
            files.delete(owner,id,3);
            assertThrows(SecurityException.class,()->files.download(owner,id));
            assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM rag_vector WHERE owner=? AND document_id=?",Integer.class,owner,id));
            System.out.println("REAL ARK ACCEPTANCE: embeddings=1024, cited answer, owner isolation, version replacement, deletion passed");
        } finally {
            // Only remove this test's UUID, never touch existing business documents.
            db.update("DELETE FROM rag_vector WHERE owner=? AND document_id=?",owner,id);
            db.update("DELETE FROM rag_revision WHERE owner=? AND document_id=?",owner,id);
            db.update("DELETE FROM rag_document WHERE owner=? AND document_id=?",owner,id);
        }
    }
}
