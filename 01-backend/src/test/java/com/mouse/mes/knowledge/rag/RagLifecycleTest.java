package com.mouse.mes.knowledge.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises real SQL transactions and filesystem operations; provider calls are explicit test doubles. */
class RagLifecycleTest {
    @TempDir Path dir;
    JdbcTemplate db;
    RagFileService files;
    RagVectorService vectors;
    StubModel model;
    static class StubModel extends RagModelClient {
        int embeddingCalls=0,answerCalls=0;
        boolean fail=false;
        Runnable onEmbed=()->{};
        StubModel(JdbcTemplate db,DataSourceTransactionManager tx){super(new ObjectMapper(),db,tx);}
        @Override public String embeddingModel(){return "test-embedding";}
        @Override public String chatModel(){return "test-chat";}
        @Override public List<double[]> embed(long owner,List<String> texts){embeddingCalls++;onEmbed.run();if(fail)throw new IllegalStateException("上游不可用");return texts.stream().map(t->new double[]{1,0,0}).toList();}
        @Override public String answer(long owner,String question,List<RagTypes.Source> sources){answerCalls++;return "操作依据[S1]。";}
    }
    @BeforeEach void setup() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1");
        db=new JdbcTemplate(source) {
            @Override public void execute(String sql) { super.execute(sql.replace("embedding JSON", "embedding VARCHAR(65535)").replace(" CHARACTER SET ascii COLLATE ascii_bin","")); }
        };
        var tx=new DataSourceTransactionManager(source);
        RagSchema schema=new RagSchema(db);schema.initialize();
        model=new StubModel(db,tx);
        files=new RagFileService(db,tx,new RagParser(),model,new ObjectMapper(),schema,dir.toString());
        vectors=new RagVectorService(db,model,new ObjectMapper());
    }
    byte[] text(String value){return value.getBytes(StandardCharsets.UTF_8);}
    @Test void uploadIsolationDownloadAndAnswer() throws Exception {
        byte[] original=text("点胶机由张师傅负责。");
        assertEquals("READY",files.upload(1,"sop-1",1,"工艺","sop.txt",original).get("status"));
        assertArrayEquals(original,files.download(1,"sop-1").bytes());
        assertTrue(files.list(2).isEmpty());
        assertThrows(SecurityException.class,()->files.download(2,"sop-1"));
        assertThrows(SecurityException.class,()->files.delete(2,"sop-1",2));
        assertEquals(1,vectors.ask(1,"谁负责点胶机？","RAG").sources().size());
        int before=model.embeddingCalls;
        assertTrue(vectors.ask(2,"谁负责点胶机？","RAG").sources().isEmpty());
        assertEquals(before,model.embeddingCalls); // No cloud request for an empty private library.
    }
    @Test void failedReplacementRetainsOldThenRetryPublishesNew() throws Exception {
        files.upload(1,"sop",1,"旧版","a.txt",text("旧内容"));
        model.fail=true;
        assertEquals("FAILED",files.upload(1,"sop",2,"新版","a.txt",text("新内容")).get("status"));
        assertEquals(1,((Number)files.detail(1,"sop").get("activeVersion")).intValue());
        assertArrayEquals(text("旧内容"),files.download(1,"sop").bytes());
        model.fail=false;
        files.retry(1,"sop",2);
        assertEquals(2,((Number)files.detail(1,"sop").get("activeVersion")).intValue());
        assertTrue(vectors.ask(1,"内容","RAG").sources().stream().allMatch(s->s.version()==2&&s.text().contains("新内容")));
        assertEquals(true,files.retry(1,"sop",2).get("unchanged"));
        assertThrows(IllegalArgumentException.class,()->files.upload(1,"sop",2,"冲突","a.txt",text("别的内容")));
    }
    @Test void tombstonePreventsRetrievalAndOldRetriesCannotDeleteNewerVersion() throws Exception {
        files.upload(1,"sop",1,"标题","a.txt",text("已删除资料"));
        files.delete(1,"sop",2);
        assertTrue(files.list(1).isEmpty());assertTrue(vectors.ask(1,"资料","RAG").sources().isEmpty());
        assertThrows(SecurityException.class,()->files.download(1,"sop"));
        assertEquals(true,files.delete(1,"sop",2).get("deleted"));
        files.upload(1,"sop",3,"重建","b.txt",text("重建内容"));
        assertThrows(IllegalArgumentException.class,()->files.delete(1,"sop",2));
        assertEquals(3,vectors.ask(1,"资料","RAG").sources().get(0).version());
    }
    @Test void invalidParsingRemainsVisibleWithoutVectors() throws Exception {
        assertEquals("FAILED",files.upload(1,"bad",1,"坏文件","test.txt",new byte[]{(byte)0xff}).get("status"));
        assertEquals("FAILED",files.list(1).get(0).get("status"));
        assertTrue(vectors.ask(1,"资料","RAG").sources().isEmpty());
        assertEquals(0,model.embeddingCalls);
    }
    @Test void inputBoundariesAndUnimplementedGraphAreExplicit() {
        assertThrows(IllegalArgumentException.class,()->RagFileService.validateId("../other"));
        assertThrows(IllegalArgumentException.class,()->files.upload(1,"a",1,"标题","a.txt",new byte[RagParser.MAX_BYTES+1]));
        assertThrows(IllegalStateException.class,()->vectors.ask(1,"问题","GRAPHRAG"));
        assertThrows(IllegalStateException.class,()->RagVectorService.cosine(new double[]{1},new double[]{1,2}));
    }
    @Test void taskThreeIndexReturnsOriginalObjectsAndRemovesSupersededChunks() throws Exception {
        var schema=new RagSchema(db);
        var bridge=new AuthorizedChunkIndex(db,model,new ObjectMapper(),schema);
        var first=new RagTypes.Chunk("build1:0","sop",1,"标题","旧版内容","正文");
        assertSame(first,bridge.retrieve(1,"问题",List.of(first),1,c->c).get(0));
        var second=new RagTypes.Chunk("build2:0","sop",2,"标题","新版内容","正文");
        assertSame(second,bridge.retrieve(1,"问题",List.of(second),1,c->c).get(0));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM rag_authorized_vector WHERE owner=1 AND chunk_id='build1:0'",Integer.class));
        assertTrue(bridge.retrieve(2,"问题",List.<RagTypes.Chunk>of(),1,c->c).isEmpty());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM rag_authorized_vector WHERE owner=2",Integer.class));
        bridge.retrieve(1,"问题",List.<RagTypes.Chunk>of(),1,c->c);
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM rag_authorized_vector WHERE owner=1",Integer.class));
        bridge.deleteDocument(1,"sop",2);
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM rag_authorized_vector WHERE owner=1",Integer.class));
    }
    @Test void narrowDocumentScopeNeverEvictsAnotherDocumentsEmbedding()throws Exception{
        var index=new AuthorizedChunkIndex(db,model,new ObjectMapper(),new RagSchema(db));
        var upper=new RagTypes.Chunk("upper:0","DocA",1,"上","设备甲由甲负责。","正文");
        var lower=new RagTypes.Chunk("lower:0","doca",1,"下","设备乙由乙负责。","正文");
        index.prepareDocument(1,List.of(upper,lower),c->c);int prepared=model.embeddingCalls;
        assertSame(upper,index.retrieve(1,"设备甲",List.of(upper),1,c->c).get(0));
        assertEquals(prepared+1,model.embeddingCalls);assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM rag_authorized_vector WHERE owner=1",Integer.class));
        index.deleteDocument(1,"DocA",1);assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM rag_authorized_vector WHERE owner=1",Integer.class));
        assertSame(lower,index.retrieve(1,"设备乙",List.of(lower),1,c->c).get(0));assertEquals(prepared+2,model.embeddingCalls);
    }
    @Test void originalIndexTableUpgradesInPlaceWithoutDroppingExistingVectors()throws Exception{
        db.execute("CREATE TABLE rag_authorized_vector(owner BIGINT NOT NULL,chunk_id VARCHAR(96) NOT NULL,fingerprint CHAR(64) NOT NULL,embedding_model VARCHAR(200) NOT NULL,embedding JSON NOT NULL,PRIMARY KEY(owner,chunk_id))");
        var chunk=new RagTypes.Chunk("legacy:0","legacy-doc",1,"标题","设备甲由甲负责。","片段1");var json=new ObjectMapper();
        String fingerprint=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(chunk)));
        db.update("INSERT INTO rag_authorized_vector VALUES(?,?,?,?,?)",1,"legacy:0",fingerprint,model.embeddingModel(),"[1,0,0]");
        var index=new AuthorizedChunkIndex(db,model,json,new RagSchema(db));assertSame(chunk,index.retrieve(1,"谁负责",List.of(chunk),1,c->c).get(0));
        assertEquals(1,model.embeddingCalls);assertEquals("legacy-doc",db.queryForObject("SELECT document_id FROM rag_authorized_vector WHERE owner=1",String.class));
    }
    @Test void progressCommitsBeforeProviderWorkAndRestoresFailureAfterServiceRecreation() throws Exception {
        model.onEmbed=()->assertEquals("EMBEDDING",files.list(1).get(0).get("stage"));
        model.fail=true;
        assertEquals("FAILED",files.upload(1,"progress",1,"进度样例","a.txt",text("设备由甲负责。")).get("status"));
        var tx=new DataSourceTransactionManager(db.getDataSource());
        var reloaded=new RagFileService(db,tx,new RagParser(),model,new ObjectMapper(),new RagSchema(db),dir.toString());
        assertEquals("FAILED",reloaded.list(1).get(0).get("stage"));
        model.fail=false;assertEquals("READY",reloaded.retry(1,"progress",1).get("status"));
        assertEquals("READY",reloaded.list(1).get(0).get("stage"));
    }
    @Test void abandonedProgressBecomesRetryableWithoutRestartAndPreservesActiveVersion() throws Exception {
        files.upload(1,"stale",1,"恢复样例","a.txt",text("设备由甲负责。"));
        db.update("UPDATE rag_revision SET status='INDEXING' WHERE owner=1 AND document_id='stale'");
        db.update("UPDATE rag_progress SET stage='GRAPH_BUILD',updated_at=TIMESTAMPADD(MINUTE,-31,CURRENT_TIMESTAMP) WHERE owner=1 AND document_id='stale'");
        var recovered=files.list(1).get(0);assertEquals("FAILED",recovered.get("status"));assertEquals("FAILED",recovered.get("stage"));
        assertEquals(1,((Number)recovered.get("activeVersion")).intValue());
        assertEquals("READY",files.retry(1,"stale",1).get("status"));
    }
}
