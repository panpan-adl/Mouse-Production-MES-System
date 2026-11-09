package com.mouse.mes.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.knowledge.rag.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.ByteArrayOutputStream;
import java.util.zip.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.mouse.mes.knowledge.KnowledgeTypes.*;

/** Actual Spring bean selection + H2 transactions + filesystem; graph/model are explicit doubles. */
class RagGraphIntegrationTest {
    AnnotationConfigApplicationContext context;
    RagFileService files;KnowledgeService knowledge;KnowledgeRepository repository;
    Neo4jKnowledgeStore graph;JdbcTemplate db;RagModelClient models;
    long owner=Math.abs(UUID.randomUUID().getMostSignificantBits()%1000000000000L)+1000000;
    static boolean real(){return "true".equals(System.getenv("KNOWLEDGE_JOINT_IT"));}
    static class Embeddings extends RagModelClient {
        int calls;
        Embeddings(ObjectMapper json,JdbcTemplate db,PlatformTransactionManager tx){super(json,db,tx);}
        @Override public String embeddingModel(){return "fixture-dense";}
        @Override public List<double[]> embed(long owner,List<String> texts){calls++;return texts.stream().map(t->new double[]{1,0,0}).toList();}
    }
    @Configuration @EnableTransactionManagement
    @Import({KnowledgeConfiguration.class,KnowledgeGraphBridge.class,KnowledgeSourceLocations.class,KnowledgeLabRepository.class})
    static class Config {
        @Bean DataSource dataSource(){
            if(real()){
                String url=System.getenv("KNOWLEDGE_IT_MYSQL_URL");
                if(url==null||!url.matches("jdbc:mysql://127\\.0\\.0\\.1:13316/mouse_mes_kg_it[?].*"))throw new IllegalStateException("Only the dedicated joint test database is allowed");
                return new DriverManagerDataSource(url,"root",System.getenv().getOrDefault("KNOWLEDGE_IT_MYSQL_PASSWORD",""));
            }
            JdbcDataSource d=new JdbcDataSource();d.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1");return d;
        }
        @Bean JdbcTemplate jdbc(DataSource data){return new JdbcTemplate(data){@Override public void execute(String sql){super.execute(real()?sql:sql.replace("embedding JSON","embedding VARCHAR(65535)"));}};}
        @Bean PlatformTransactionManager manager(DataSource data){return new DataSourceTransactionManager(data);}
        @Bean ObjectMapper json(){return new ObjectMapper();}
        @Bean RagSchema ragSchema(JdbcTemplate db){return new RagSchema(db);}
        @Bean RagModelClient embeddings(ObjectMapper json,JdbcTemplate db,PlatformTransactionManager tx){return new Embeddings(json,db,tx);}
        @Bean RagParser parser(){return new RagParser();}
        @Bean AuthorizedChunkIndex index(JdbcTemplate db,RagModelClient model,ObjectMapper json,RagSchema schema){return spy(new AuthorizedChunkIndex(db,model,json,schema));}
        @Bean RagFileService files(JdbcTemplate db,PlatformTransactionManager tx,RagParser parser,RagModelClient model,ObjectMapper json,RagSchema schema) throws Exception {
            db.execute("CREATE TABLE IF NOT EXISTS mm_kg_document(owner_id BIGINT NOT NULL,document_id VARCHAR(64) NOT NULL,title VARCHAR(200),version BIGINT,content MEDIUMTEXT,build_id VARCHAR(36),chunks_json MEDIUMTEXT,state VARCHAR(16),updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(owner_id,document_id))");
            return new RagFileService(db,tx,parser,model,json,schema,Files.createTempDirectory("mousemes-joint-").toString());
        }
        @Bean KnowledgeSettings settings(){KnowledgeSettings s=new KnowledgeSettings();s.setEnabled(true);return s;}
        @Bean KnowledgeRepository repository(JdbcTemplate db,ObjectMapper json){return new KnowledgeRepository(db,json);}
        @Bean Neo4jKnowledgeStore graph(){
            if(!real())return mock(Neo4jKnowledgeStore.class);
            KnowledgeSettings s=new KnowledgeSettings();s.setNeo4jUrl("http://127.0.0.1:17474");s.setNeo4jPassword(System.getenv("KNOWLEDGE_IT_NEO4J_PASSWORD"));
            return spy(new Neo4jKnowledgeStore(s,new ObjectMapper()));
        }
        @Bean KnowledgeModel model(){KnowledgeModel m=mock(KnowledgeModel.class);when(m.extract(anyString())).thenAnswer(call->{String text=call.getArgument(0);String person=text.contains("由乙")?"乙":"甲";return List.of(new Triple("设备","负责人",person,"设备由"+person+"负责。"));});when(m.answer(anyString(),anyList(),anyList())).thenReturn("设备由甲负责。[S1]");return m;}
        @Bean KnowledgeService knowledge(KnowledgeSettings settings,KnowledgeRepository repo,Neo4jKnowledgeStore graph,KnowledgeModel model,RagRetriever rag){return new KnowledgeService(settings,repo,graph,model,rag);}
    }
    @BeforeEach void setup(){
        context=new AnnotationConfigApplicationContext();context.getEnvironment().getSystemProperties().put("mes.knowledge.enabled","true");context.register(Config.class);context.refresh();
        files=context.getBean(RagFileService.class);knowledge=context.getBean(KnowledgeService.class);repository=context.getBean(KnowledgeRepository.class);graph=context.getBean(Neo4jKnowledgeStore.class);db=context.getBean(JdbcTemplate.class);models=context.getBean(RagModelClient.class);
    }
    @AfterEach void close(){
        try {
            if(files!=null){
                reset(graph);reset(context.getBean(AuthorizedChunkIndex.class));
                for(var doc:files.list(owner))files.delete(owner,doc.get("documentId").toString(),((Number)doc.get("version")).intValue()+1);
                if(real())graph.delete(owner,"joint-doc",Integer.MAX_VALUE);
                for(String table:List.of("rag_progress","rag_vector","rag_authorized_vector","rag_graph_sync","rag_revision","rag_document"))db.update("DELETE FROM "+table+" WHERE owner=?",owner);
                db.update("DELETE FROM mm_kg_document WHERE owner_id=?",owner);
            }
        }finally{if(context!=null)context.close();System.clearProperty("mes.knowledge.enabled");}
    }
    Map<String,Object> upload(int version,String text) throws Exception{return files.upload(owner,"joint-doc",version,"联合工艺","sop.txt",text.getBytes(StandardCharsets.UTF_8));}
    @Test void actualBeanAssemblyUsesVectorRetrieverAndSharedChunkIds() throws Exception {
        assertEquals(1,context.getBeansOfType(RagRetriever.class).size());assertInstanceOf(VectorRagRetriever.class,context.getBean(RagRetriever.class));
        var response=upload(1,"设备由甲负责。");assertEquals("READY",response.get("graphStatus"));assertTrue(files.graphIntegrated());assertEquals("READY",files.list(owner).get(0).get("stage"));
        var document=repository.list(owner).get(0);Answer reply=knowledge.ask(owner,new Question("设备由谁负责？","RAG"));
        assertEquals(document.chunks().get(0).id(),reply.sources().get(0).chunkId());assertTrue(reply.retrieval().contains("fixture-dense"));
        assertEquals("正文",reply.sources().get(0).location());assertEquals(2,((Embeddings)models).calls);
        assertArrayEquals("设备由甲负责。".getBytes(StandardCharsets.UTF_8),files.download(owner,"joint-doc").bytes());
        if(real()){
            assertFalse(knowledge.visualize(owner).facts().isEmpty());assertEquals("正文",knowledge.visualize(owner).sources().get(0).location());
            assertEquals(document.chunks().get(0).id(),knowledge.ask(owner,new Question("设备","GRAPHRAG")).sources().get(0).chunkId());
            assertEquals(document.chunks().get(0).id(),knowledge.ask(owner,new Question("设备","HYBRID")).sources().get(0).chunkId());
        }
    }
    @Test void graphFailureRollsBackBothActiveVersionsThenRetryPublishes() throws Exception {
        upload(1,"设备由甲负责。");String old=repository.list(owner).get(0).buildId();
        doThrow(new IllegalStateException("Neo4j离线")).when(graph).publish(eq(owner),eq("joint-doc"),eq(2L),anyString(),anyList());
        assertEquals("FAILED",upload(2,"设备由乙负责。").get("status"));
        assertEquals(1,((Number)files.detail(owner,"joint-doc").get("activeVersion")).intValue());assertEquals(old,repository.list(owner).get(0).buildId());
        assertEquals(1,context.getBean(KnowledgeLabRepository.class).versions(owner,"joint-doc").size());
        assertArrayEquals("设备由甲负责。".getBytes(StandardCharsets.UTF_8),files.download(owner,"joint-doc").bytes());
        if(real())doCallRealMethod().when(graph).publish(eq(owner),eq("joint-doc"),eq(2L),anyString(),anyList());
        else doNothing().when(graph).publish(eq(owner),eq("joint-doc"),eq(2L),anyString(),anyList());
        assertEquals("READY",files.retry(owner,"joint-doc",2).get("graphStatus"));assertEquals(2,repository.list(owner).get(0).version());
        assertEquals(2,context.getBean(KnowledgeLabRepository.class).versions(owner,"joint-doc").size());
        if(real())assertTrue(knowledge.visualize(owner).facts().stream().allMatch(f->f.target().equals("乙")));
    }
    @Test void deleteRevokesBothIndicesBeforePhysicalCleanupAndCanRetry() throws Exception {
        upload(1,"设备由甲负责。");knowledge.ask(owner,new Question("设备","RAG"));
        doThrow(new IllegalStateException("Neo4j离线")).when(graph).delete(owner,"joint-doc",2);
        assertEquals(true,files.delete(owner,"joint-doc",2).get("graphCleanupPending"));
        assertTrue(files.list(owner).isEmpty());assertTrue(repository.list(owner).isEmpty());
        assertTrue(context.getBean(KnowledgeLabRepository.class).versions(owner,"joint-doc").isEmpty());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM rag_vector WHERE owner=?",Integer.class,owner));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM rag_authorized_vector WHERE owner=?",Integer.class,owner));
        if(real())doCallRealMethod().when(graph).delete(owner,"joint-doc",2);
        else doNothing().when(graph).delete(owner,"joint-doc",2);
        assertEquals(false,files.retry(owner,"joint-doc",2).get("graphCleanupPending"));
    }
    @Test void failedGraphRevocationDoesNotDeleteOnlyOneSide() throws Exception {
        upload(1,"设备由甲负责。");
        KnowledgeModel model=context.getBean(KnowledgeModel.class);
        // A failure after graph tombstone SQL but before file revocation must roll back the outer transaction.
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("fixture cache failure")).when(context.getBean(AuthorizedChunkIndex.class)).evict(eq(owner),anyList());
        assertThrows(Exception.class,()->files.delete(owner,"joint-doc",2));
        assertEquals(1,files.list(owner).size());assertEquals(1,repository.list(owner).size());
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM rag_vector WHERE owner=?",Integer.class,owner));
        verify(model).extract(anyString());
    }
    @Test void ownerAndOldEventCannotBypassJointPublication() throws Exception {
        upload(1,"设备由甲负责。");assertThrows(SecurityException.class,()->files.download(owner+1,"joint-doc"));
        upload(2,"设备由乙负责。");assertThrows(IllegalArgumentException.class,()->files.retry(owner,"joint-doc",1));
        assertEquals(2,repository.list(owner).get(0).version());
    }
    @Test void docxOriginalAndParagraphLocationsSurviveUnifiedRetrieval() throws Exception {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(output)){
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>"
                    +"<w:p><w:r><w:t>设备由甲负责。</w:t></w:r></w:p><w:p><w:r><w:t>设备每日点检。</w:t></w:r></w:p></w:body></w:document>").getBytes(StandardCharsets.UTF_8));
        }
        byte[] original=output.toByteArray();
        assertEquals("READY",files.upload(owner,"joint-doc",1,"设备规范","sop.docx",original).get("graphStatus"));
        var result=knowledge.ask(owner,new Question("设备负责人","RAG"));
        assertTrue(result.sources().get(0).location().contains("段落1"));assertTrue(result.sources().get(0).location().contains("段落2"));
        assertArrayEquals(original,files.download(owner,"joint-doc").bytes());
    }
    @Test void parsersAgreeOnSharedUnicodeChunkBoundaries() {
        String text="甲".repeat(1799)+"😀"+"乙".repeat(2500);
        var raw=new RagParser().chunks("unicode",1,"标题","shared",List.of(new RagTypes.Part(text,"正文")));
        var graphChunks=KnowledgeService.chunks(new DocumentInput("unicode","标题",1,text),"shared");
        assertEquals(graphChunks.size(),raw.size());
        for(int i=0;i<raw.size();i++){assertEquals(graphChunks.get(i).id(),raw.get(i).id());assertEquals(graphChunks.get(i).text(),raw.get(i).text());}
    }
}
