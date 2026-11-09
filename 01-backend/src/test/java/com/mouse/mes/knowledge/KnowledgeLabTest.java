package com.mouse.mes.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static com.mouse.mes.knowledge.KnowledgeTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KnowledgeLabTest {
    JdbcTemplate db;KnowledgeLabRepository repo;KnowledgeRepository docs;KnowledgeService knowledge;Neo4jKnowledgeStore graph;KnowledgeModel model;KnowledgeSettings settings;KnowledgeLabService lab;
    final Fact old=new Fact("点胶机DG01","负责人","甲","点胶机DG01由甲负责。","old:0");
    final Fact fresh=new Fact("点胶机DG01","负责人","乙","点胶机DG01由乙负责。","new:0");
    Document document(String build){return new Document("doc","工艺",build.equals("new")?2:1,"正文",build,List.of(new Chunk(build+":0","doc","工艺",1,0,"正文")),"READY");}
    Answer answer(){return new Answer("甲负责[S1]","GRAPHRAG","图谱",List.of(new Evidence("S1","doc","工艺",1,"old:0","正文")),List.of(old),"fixture");}
    @BeforeEach void setup(){
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");db=new JdbcTemplate(source);repo=new KnowledgeLabRepository(db,new ObjectMapper());
        docs=mock(KnowledgeRepository.class);knowledge=mock(KnowledgeService.class);graph=mock(Neo4jKnowledgeStore.class);model=mock(KnowledgeModel.class);settings=new KnowledgeSettings();settings.setEnabled(true);
        when(docs.list(7)).thenReturn(List.of(document("old")));when(docs.list(8)).thenReturn(List.of());
        when(graph.entities(7,List.of("old"))).thenReturn(List.of("点胶机DG01","点胶设备DG01","点胶机DG02","装配台AS01","甲"));
        when(knowledge.ask(eq(7L),any())).thenReturn(answer());lab=new KnowledgeLabService(knowledge,docs,repo,graph,model,settings);
    }
    @Test void differenceIgnoresChunkAndQuoteChangesButSeparatesChangedTargets(){
        var same=new Fact(old.source(),old.relation(),old.target(),"另一段证据","new:2");
        var result=KnowledgeLabService.difference(1,2,List.of(old,old),List.of(same,fresh));
        assertEquals(List.of(fresh),result.get("added"));assertEquals(List.of(),result.get("removed"));assertEquals(List.of(same),result.get("retained"));
    }
    @Test void snapshotsAreOwnerScopedAndPublicationRollbackDoesNotCreateHistory(){
        repo.snapshot(7,"doc",1,"工艺",List.of(old));assertEquals(List.of(old),repo.facts(7,"doc",1));assertThrows(IllegalArgumentException.class,()->repo.facts(8,"doc",1));
        new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource())).executeWithoutResult(tx->{repo.snapshot(7,"doc",2,"工艺",List.of(fresh));tx.setRollbackOnly();});
        assertEquals(1,repo.versions(7,"doc").size());assertThrows(SecurityException.class,()->lab.versions(8,"doc"));
        repo.purge(7,"doc");assertTrue(repo.versions(7,"doc").isEmpty());
    }
    @Test void historicalDiffRequiresCurrentPermissionAndValidOrderedVersions(){
        repo.snapshot(7,"doc",1,"工艺",List.of(old));repo.snapshot(7,"doc",2,"工艺",List.of(fresh));
        assertEquals(List.of(fresh),lab.diff(7,"doc",1,2).get("added"));assertThrows(SecurityException.class,()->lab.diff(8,"doc",1,2));assertThrows(IllegalArgumentException.class,()->lab.diff(7,"doc",2,1));
        when(docs.list(7)).thenReturn(List.of());assertThrows(SecurityException.class,()->lab.diff(7,"doc",1,2));
    }
    @Test void approvedAliasesOnlyExpandAuthorizedCandidatesAndAreRevocable(){
        lab.review(7,new KnowledgeLabService.Review("点胶设备DG01","EQUIPMENT","点胶机DG01"));
        assertEquals(Set.of("点胶设备DG01","点胶机DG01"),new HashSet<>(repo.seeds(7,"点胶设备DG01是谁负责？",List.of("点胶设备DG01","点胶机DG01"))));
        assertTrue(repo.seeds(8,"点胶设备DG01",List.of("点胶设备DG01","点胶机DG01")).isEmpty());assertTrue(repo.seeds(7,"点胶设备DG01",List.of("点胶设备DG01")).isEmpty());
        lab.review(7,new KnowledgeLabService.Review("点胶设备DG01",null,""));assertTrue(repo.aliases(7).isEmpty());
    }
    @Test void aliasesRejectDifferentModelsTypesCyclesAndForeignEntities(){
        assertThrows(IllegalArgumentException.class,()->lab.review(7,new KnowledgeLabService.Review("点胶机DG01",null,"点胶机DG02")));
        assertThrows(SecurityException.class,()->lab.review(8,new KnowledgeLabService.Review("点胶机DG01","EQUIPMENT",null)));
        lab.review(7,new KnowledgeLabService.Review("点胶机DG01","EQUIPMENT",null));lab.review(7,new KnowledgeLabService.Review("点胶设备DG01","PROCESS",null));
        assertThrows(IllegalArgumentException.class,()->lab.review(7,new KnowledgeLabService.Review("点胶设备DG01",null,"点胶机DG01")));
        lab.review(7,new KnowledgeLabService.Review("点胶设备DG01","EQUIPMENT","点胶机DG01"));
        assertThrows(IllegalArgumentException.class,()->lab.review(7,new KnowledgeLabService.Review("点胶机DG01",null,"点胶设备DG01")));
        assertThrows(IllegalArgumentException.class,()->lab.review(7,new KnowledgeLabService.Review("点胶机DG01","PROCESS",null)));
    }
    @Test void comparisonUsesSameScopeReportsPartialFailuresAndRejectsVersionRaces(){
        when(knowledge.ask(eq(7L),argThat(q->q.mode().equals("RAG")))).thenThrow(new IllegalStateException("嵌入模型限额"));
        var result=lab.compare(7,new Question("负责人？","HYBRID",List.of("doc")));assertEquals(3,((List<?>)result.get("results")).size());
        var rows=(List<Map<String,Object>>)result.get("results");assertEquals(false,rows.get(0).get("success"));assertEquals(true,rows.get(1).get("success"));
        when(docs.list(7)).thenReturn(List.of(document("old")),List.of(document("new")));
        assertThrows(IllegalStateException.class,()->lab.compare(7,new Question("负责人？","RAG",List.of("doc"))));
    }
    @Test void dialogueContextCannotBeForgedOrReusedAfterUpdateOrScopeChange(){
        var response=lab.dialogue(7,new KnowledgeLabService.DialogueInput("点胶机由谁负责？","GRAPHRAG",List.of("doc"),null));String id=response.get("contextId").toString();
        assertThrows(SecurityException.class,()->repo.context(8,id));
        when(model.resolveFollowup(anyString(),anyString(),anyString())).thenReturn("点胶机要做什么点检？");
        var next=lab.dialogue(7,new KnowledgeLabService.DialogueInput("它要做什么点检？","GRAPHRAG",List.of("doc"),id));assertEquals("点胶机要做什么点检？",next.get("resolvedQuestion"));
        verify(knowledge).ask(7,new Question("点胶机要做什么点检？","GRAPHRAG",List.of("doc")));
        when(docs.list(7)).thenReturn(List.of(document("new")));assertThrows(IllegalStateException.class,()->lab.dialogue(7,new KnowledgeLabService.DialogueInput("它呢？","RAG",List.of("doc"),id)));
        db.update("UPDATE mm_kg_dialogue SET created_at=TIMESTAMPADD(HOUR,-3,CURRENT_TIMESTAMP)");assertThrows(SecurityException.class,()->repo.context(7,id));
    }
    @Test void contextsAndSnapshotsAreRevokedOnDocumentDeletion(){
        var id=lab.dialogue(7,new KnowledgeLabService.DialogueInput("谁负责？","GRAPHRAG",null,null)).get("contextId").toString();repo.snapshot(7,"doc",1,"工艺",List.of(old));repo.purge(7,"doc");
        assertThrows(SecurityException.class,()->repo.context(7,id));assertTrue(repo.versions(7,"doc").isEmpty());
    }
    @Test void associationGroupsEvidenceWithoutChangingOriginalSources(){
        Graph result=new Graph(List.of(old),false,answer().sources());when(knowledge.visualizeSeeds(7,List.of("doc"),"点胶机DG01",List.of())).thenReturn(result);
        var response=lab.association(7,"点胶机DG01",List.of("doc"));assertEquals(result,response.get("graph"));assertThrows(IllegalArgumentException.class,()->lab.association(7,"不存在设备",List.of("doc")));
    }
    @Test void disabledLabDoesNotReadPrivateData(){settings.setEnabled(false);assertThrows(IllegalStateException.class,()->lab.quality(7,null));verifyNoInteractions(docs,graph);}
}
