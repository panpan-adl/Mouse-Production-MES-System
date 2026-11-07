package com.mouse.mes.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.mouse.mes.knowledge.KnowledgeTypes.*;

class KnowledgeServiceTest {
    KnowledgeSettings settings; KnowledgeRepository repo; Neo4jKnowledgeStore graph; KnowledgeModel model; KnowledgeService service;
    DocumentInput input=new DocumentInput("sop-a100","A100工艺",1,"A100鼠标需要点胶工序。点胶工序使用点胶机。点胶机需要点检。");
    @BeforeEach void setup() {
        settings=new KnowledgeSettings();settings.setEnabled(true);settings.setModel("test-model");
        repo=mock(KnowledgeRepository.class); graph=mock(Neo4jKnowledgeStore.class);model=mock(KnowledgeModel.class);
        service=new KnowledgeService(settings,repo,graph,model,new LexicalRagRetriever());
    }
    Document doc(long version,String state) {
        return new Document(input.documentId(),input.title(),version,input.text(),"active-build",KnowledgeService.chunks(input,"active-build"),state);
    }
    @Test void disabledModuleNeverReadsPrivateDatabase() {
        settings.setEnabled(false);
        assertThrows(IllegalStateException.class,()->service.documents(7));verifyNoInteractions(repo,graph,model);
    }
    @Test void sameVersionRetryDoesNotDuplicateGraph() {
        when(repo.lock(7,"sop-a100")).thenReturn(doc(1,"READY"));
        assertEquals(true,service.upsert(7,input).get("unchanged"));verifyNoInteractions(graph,model);
    }
    @Test void conflictingOrOlderVersionIsRejected() {
        when(repo.lock(7,"sop-a100")).thenReturn(doc(1,"READY"));
        assertThrows(IllegalArgumentException.class,()->service.upsert(7,new DocumentInput("sop-a100","另一标题",1,input.text())));
        verifyNoInteractions(graph,model);
    }
    @Test void graphFailureDoesNotPublishNewActiveVersion() {
        when(repo.lock(7,"sop-a100")).thenReturn(doc(0,"NEW"));when(model.extract(anyString())).thenReturn(List.of());
        doThrow(new IllegalStateException("offline")).when(graph).publish(anyLong(),anyString(),anyLong(),anyString(),anyList());
        assertThrows(IllegalStateException.class,()->service.upsert(7,input));verify(repo,never()).save(anyLong(),any());
    }
    @Test void publishesAfterSuccessfulExtractionAndNeo4jWrite() {
        when(repo.lock(7,"sop-a100")).thenReturn(doc(0,"NEW"));
        when(model.extract(anyString())).thenReturn(List.of(new Triple("A100鼠标","需要","点胶工序","A100鼠标需要点胶工序。")));
        assertEquals(1,service.upsert(7,input).get("relationCount"));
        var order=inOrder(graph,repo);order.verify(graph).publish(eq(7L),eq("sop-a100"),eq(1L),anyString(),anyList());order.verify(repo).save(eq(7L),any());
    }
    @Test void deleteCommitsTombstoneWithoutDependingOnNeo4jAvailability() {
        when(repo.lock(7,"sop-a100")).thenReturn(doc(1,"READY"));service.tombstone(7,"sop-a100",2);
        ArgumentCaptor<Document> saved=ArgumentCaptor.forClass(Document.class);verify(repo).save(eq(7L),saved.capture());
        assertEquals("DELETED_PENDING",saved.getValue().state());assertEquals("",saved.getValue().text());verifyNoInteractions(graph);
        doThrow(new IllegalStateException("offline")).when(graph).delete(7,"sop-a100",2);
        assertEquals(true,service.cleanup(7,"sop-a100",2).get("cleanupPending"));
    }
    @Test void staleDeleteCannotRemoveNewerVersion() {
        when(repo.lock(7,"sop-a100")).thenReturn(doc(3,"READY"));
        assertThrows(IllegalArgumentException.class,()->service.tombstone(7,"sop-a100",2));verifyNoInteractions(graph);
    }
    @Test void repeatedDeleteDoesNotRecreateData() {
        when(repo.lock(7,"sop-a100")).thenReturn(doc(2,"DELETED"));service.tombstone(7,"sop-a100",2);verify(repo,never()).save(anyLong(),any());
    }
    @Test void ownerAndActiveBuildArePassedToGraphSearch() {
        when(repo.list(7)).thenReturn(List.of(doc(1,"READY")));
        when(graph.graph(eq(7L),eq(List.of("active-build")),anyString())).thenReturn(new Graph(List.of(),false));
        assertTrue(service.ask(7,new Question("A100鼠标有哪些工序？","GRAPHRAG")).sources().isEmpty());
        verify(graph).graph(7,List.of("active-build"),"A100鼠标有哪些工序？");verifyNoInteractions(model);
    }
    @Test void graphEvidenceIsGroundedInAuthorizedCurrentChunks() {
        Document d=doc(1,"READY");when(repo.list(7)).thenReturn(List.of(d));
        Fact owned=new Fact("A100鼠标","需要","点胶工序","A100鼠标需要点胶工序。",d.chunks().get(0).id());
        Fact foreign=new Fact("其他用户","私有","泄漏","不该出现","foreign-chunk");
        when(graph.graph(eq(7L),anyList(),anyString())).thenReturn(new Graph(List.of(owned,foreign),false));
        when(model.answer(anyString(),anyList(),anyList())).thenReturn("A100鼠标需要点胶工序。[S1]");
        Answer result=service.ask(7,new Question("A100鼠标","GRAPHRAG"));
        assertEquals(1,result.sources().size());assertEquals(List.of(owned),result.facts());assertEquals("sop-a100",result.sources().get(0).documentId());
    }
    @Test void extensionCannotReturnForeignOrModifiedChunks() {
        RagRetriever rag=mock(RagRetriever.class);when(repo.list(7)).thenReturn(List.of(doc(1,"READY")));
        when(rag.retrieve(anyLong(),anyString(),anyList(),anyInt())).thenReturn(List.of(new Chunk("foreign","x","秘密",1,0,"秘密")));
        var extended=new KnowledgeService(settings,repo,graph,model,rag);
        assertThrows(SecurityException.class,()->extended.ask(7,new Question("问题","RAG")));verifyNoInteractions(model);
    }
    @Test void ragModeDoesNotCallGraphAndShowsBaselineLabel() {
        when(repo.list(7)).thenReturn(List.of(doc(1,"READY")));when(model.answer(anyString(),anyList(),anyList())).thenReturn("需要点胶。[S1]");
        Answer result=service.ask(7,new Question("A100鼠标","RAG"));assertTrue(result.retrieval().contains("关键词基线"));verifyNoInteractions(graph);
    }
    @Test void crossOwnerDocumentReadDoesNotFallBackToAnotherOwner() {
        when(repo.list(8)).thenReturn(List.of());assertThrows(IllegalArgumentException.class,()->service.document(8,"sop-a100"));verify(repo).list(8);
    }
    @Test void hybridCombinesGraphAndTextWithoutDuplicateSources() {
        Document d=doc(1,"READY");when(repo.list(7)).thenReturn(List.of(d));
        when(graph.graph(eq(7L),anyList(),anyString())).thenReturn(new Graph(List.of(new Fact("A100鼠标","需要","点胶工序","A100鼠标需要点胶工序。",d.chunks().get(0).id())),false));
        when(model.answer(anyString(),anyList(),anyList())).thenReturn("需要点胶。[S1]");
        assertEquals(1,service.ask(7,new Question("A100鼠标","HYBRID")).sources().size());
    }
    @Test void chunkingPreservesTextWithOverlapAndUnicode() {
        String text="甲".repeat(1799)+"😀"+"乙".repeat(4000);
        var chunks=KnowledgeService.chunks(new DocumentInput("unicode","标题",1,text),"build");
        assertEquals(4,chunks.size());
        for(Chunk c:chunks) { assertFalse(Character.isLowSurrogate(c.text().charAt(0)));assertFalse(Character.isHighSurrogate(c.text().charAt(c.text().length()-1)));assertTrue(text.contains(c.text())); }
        assertTrue(chunks.get(chunks.size()-1).text().endsWith("乙"));
    }
    @Test void extractionRejectsInventedEntitiesQuotesAndMalformedJson() throws Exception {
        ObjectMapper json=new ObjectMapper();
        assertThrows(IllegalStateException.class,()->KnowledgeModel.validate(json.readTree("{\"triples\":[] ,\"x\":1}").path("missing"),input.text()));
        assertThrows(IllegalStateException.class,()->KnowledgeModel.validate(json.readTree("{\"triples\":[{\"source\":\"不存在\",\"relation\":\"需要\",\"target\":\"点胶机\",\"quote\":\"点胶机需要点检。\"}]}"),input.text()));
        assertThrows(IllegalStateException.class,()->KnowledgeModel.validate(json.readTree("{\"triples\":[{\"source\":\"点胶机\",\"relation\":\"需要\",\"target\":\"点检\",\"quote\":\"伪造引用\"}]}"),input.text()));
        assertEquals(0,KnowledgeModel.validate(json.readTree("{\"triples\":[]}"),input.text()).size());
    }
    @Test void modelMustCiteRealSourceIds() {
        List<Evidence> sources=List.of(new Evidence("S1","doc","标题",1,"chunk","原文"));
        assertThrows(IllegalStateException.class,()->KnowledgeService.validateCitations("答案没有引用",sources));
        assertThrows(IllegalStateException.class,()->KnowledgeService.validateCitations("错误来源[S2]",sources));
        assertDoesNotThrow(()->KnowledgeService.validateCitations("有依据[S1]",sources));
    }
    @Test void rejectsInvalidPayloadsBeforeAccessingStorage() {
        assertThrows(IllegalArgumentException.class,()->service.upsert(7,new DocumentInput("../escape","title",1,"text")));
        assertThrows(IllegalArgumentException.class,()->service.upsert(7,new DocumentInput("id","title",0,"text")));
        assertThrows(IllegalArgumentException.class,()->service.upsert(7,new DocumentInput("id","title",1,"x".repeat(12001))));
        assertThrows(IllegalArgumentException.class,()->service.ask(7,new Question("问题","SQL")));verifyNoInteractions(repo,graph,model);
    }
    @Test void requestedScopeCannotIncludeAnotherUsersFile() {
        when(repo.list(7)).thenReturn(List.of(doc(1,"READY")));
        assertThrows(SecurityException.class,()->service.ask(7,new Question("问题","GRAPHRAG",List.of("private-other"))));
        assertThrows(SecurityException.class,()->service.visualize(7,List.of("private-other"),""));verifyNoInteractions(graph,model);
    }
    @Test void selectedDocumentLimitsGraphAndRagInputs() {
        Document d=doc(1,"READY");
        Document other=new Document("other","其他资料",1,"别的内容","other-build",List.of(),"READY");
        when(repo.list(7)).thenReturn(List.of(d,other));when(graph.graph(eq(7L),anyList(),anyString())).thenReturn(new Graph(List.of(),false));
        service.ask(7,new Question("问题","GRAPHRAG",List.of(d.documentId())));
        verify(graph).graph(7,List.of("active-build"),"问题");
    }
    @Test void graphEntitySearchUsesSelectedBuilds() {
        when(repo.list(7)).thenReturn(List.of(doc(1,"READY")));when(graph.graph(anyLong(),anyList(),anyString())).thenReturn(new Graph(List.of(),false));
        service.visualize(7,List.of("sop-a100")," 点胶机 ");verify(graph).graph(7,List.of("active-build"),"点胶机");
    }
    @Test void overlappingChunkFactsAreDeduplicatedBeforePublishing() {
        var longInput=new DocumentInput("sop-a100","标题",1,"点胶工序使用点胶机。".repeat(300));
        when(repo.lock(7,"sop-a100")).thenReturn(doc(0,"NEW"));
        when(model.extract(anyString())).thenReturn(List.of(new Triple("点胶工序","使用","点胶机","点胶工序使用点胶机。")));
        assertEquals(1,service.upsert(7,longInput).get("relationCount"));
    }
    @Test void vectorExtensionMustObserveRecallLimit() {
        var d=doc(1,"READY");when(repo.list(7)).thenReturn(List.of(d));RagRetriever rag=mock(RagRetriever.class);
        when(rag.retrieve(anyLong(),anyString(),anyList(),anyInt())).thenReturn(Collections.nCopies(7,d.chunks().get(0)));
        var extended=new KnowledgeService(settings,repo,graph,model,rag);
        assertThrows(IllegalStateException.class,()->extended.ask(7,new Question("问题","RAG")));verifyNoInteractions(model);
    }
    @Test void completedCleanupAcknowledgesOnlyItsExactDeletionVersion() {
        service.cleanup(7,"sop-a100",2);verify(repo).finishDeletion(7,"sop-a100",2);verify(graph).delete(7,"sop-a100",2);
    }
    @Test void pendingDeletionRetryDoesNotCreateAnotherVersion() {
        when(repo.lock(7,"sop-a100")).thenReturn(doc(2,"DELETED_PENDING"));service.tombstone(7,"sop-a100",2);verify(repo,never()).save(anyLong(),any());
    }
    @Test void modelEntityLinkingIsUsedOnlyWhenExactGraphSearchMisses() {
        settings.setModelBaseUrl("http://model");settings.setModelApiKey("fixture");Document d=doc(1,"READY");when(repo.list(7)).thenReturn(List.of(d));
        when(graph.graph(eq(7L),anyList(),anyString())).thenReturn(new Graph(List.of(),false));
        when(graph.entities(7,List.of("active-build"))).thenReturn(List.of("点胶机"));when(model.linkEntities("点胶设备",List.of("点胶机"))).thenReturn(List.of("点胶机"));
        Fact fact=new Fact("点胶机","需要","点检","点胶机需要点检。",d.chunks().get(0).id());
        when(graph.graph(7,List.of("active-build"),"点胶设备",List.of("点胶机"))).thenReturn(new Graph(List.of(fact),false));
        when(model.answer(anyString(),anyList(),anyList())).thenReturn("需要点检。[S1]");
        Answer answer=service.ask(7,new Question("点胶设备","GRAPHRAG"));assertTrue(answer.retrieval().contains("模型定位实体：点胶机"));assertEquals(1,answer.sources().size());
    }
    @Test void deletedOrChangedSourcesCannotBeReturnedAfterSlowModelResponse() {
        Document d=doc(1,"READY");when(repo.list(7)).thenReturn(List.of(d),List.of());
        Fact fact=new Fact("点胶机","需要","点检","点胶机需要点检。",d.chunks().get(0).id());
        when(graph.graph(eq(7L),anyList(),anyString())).thenReturn(new Graph(List.of(fact),false));when(model.answer(anyString(),anyList(),anyList())).thenReturn("需要点检。[S1]");
        assertThrows(IllegalStateException.class,()->service.ask(7,new Question("点胶机","GRAPHRAG")));
    }
    @Test void exactInsufficientResponseIsAllowedWithoutInventingCitations() {
        assertDoesNotThrow(()->KnowledgeService.validateCitations(KnowledgeModel.INSUFFICIENT,List.of()));
        assertThrows(IllegalStateException.class,()->KnowledgeService.validateCitations("当前资料不足，但是负责人是张三。",List.of()));
    }
    @Test void invalidCitationGetsOneRepairAttemptAndStillMustValidate() {
        Document d=doc(1,"READY");when(repo.list(7)).thenReturn(List.of(d));
        when(model.answer(anyString(),anyList(),anyList())).thenReturn("没有来源的结论",KnowledgeModel.INSUFFICIENT);
        Answer answer=service.ask(7,new Question("A100鼠标","RAG"));assertEquals(KnowledgeModel.INSUFFICIENT,answer.answer());
        verify(model,times(2)).answer(anyString(),anyList(),anyList());
    }
}
