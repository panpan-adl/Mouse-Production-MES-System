package com.mouse.mes.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.util.regex.*;
import static com.mouse.mes.knowledge.KnowledgeTypes.*;

@Service
public class KnowledgeService {
    private final KnowledgeSettings settings;
    private final KnowledgeRepository repository;
    private final Neo4jKnowledgeStore graph;
    private final KnowledgeModel model;
    private final RagRetriever rag;
    @Autowired(required=false) private KnowledgeSourceLocations sourceLocations;
    @Autowired(required=false) private KnowledgeLabRepository lab;
    public KnowledgeService(KnowledgeSettings settings, KnowledgeRepository repository, Neo4jKnowledgeStore graph, KnowledgeModel model, RagRetriever rag) {
        this.settings=settings; this.repository=repository; this.graph=graph; this.model=model; this.rag=rag;
    }
    public Map<String,Object> status(boolean probe) {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("enabled",settings.isEnabled()); result.put("modelConfigured",settings.modelConfigured());
        result.put("model",settings.getModel()); result.put("ragProvider",rag.name()); result.put("neo4j","未检测");
        result.put("revision","task2-task3-r1");result.put("vectorRetrieverConnected",!(rag instanceof LexicalRagRetriever));
        result.put("supportedModes",List.of("RAG","GRAPHRAG","HYBRID"));result.put("maxDocumentCharacters",12000);
        if(probe && settings.isEnabled()) {
            try { graph.ping(); result.put("neo4j","已连接"); }
            catch(IllegalStateException e) { result.put("neo4j",e.getMessage()); }
        }
        return result;
    }
    public List<Map<String,Object>> documents(long owner) {
        settings.requireEnabled();
        return repository.list(owner).stream().map(d->Map.<String,Object>of("documentId",d.documentId(),"title",d.title(),
                "version",d.version(),"chunkCount",d.chunks().size(),"state",d.state())).toList();
    }
    public DocumentInput document(long owner,String id) {
        settings.requireEnabled(); id(id);
        Document doc=repository.list(owner).stream().filter(d->d.documentId().equals(id)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("文件不存在或无权访问"));
        return new DocumentInput(doc.documentId(),doc.title(),doc.version(),doc.text());
    }
    @Transactional
    public Map<String,Object> upsert(long owner,DocumentInput input) {
        settings.requireEnabled(); validate(input);
        Document old=repository.lock(owner,input.documentId());
        if(input.version()==old.version() && old.state().equals("READY") && old.text().equals(input.text()) && old.title().equals(input.title()))
            return Map.of("documentId",old.documentId(),"version",old.version(),"unchanged",true);
        if(input.version()<=old.version()) throw new IllegalArgumentException("版本已过期或同版本内容冲突，请使用更高版本重试");
        String build=UUID.randomUUID().toString();
        List<Chunk> chunks=chunks(input,build);
        Map<Triple,Fact> uniqueFacts=new LinkedHashMap<>();
        for(Chunk chunk:chunks) for(Triple t:model.extract(chunk.text()))
            uniqueFacts.putIfAbsent(t,new Fact(t.source(),t.relation(),t.target(),t.quote(),chunk.id()));
        List<Fact> facts=List.copyOf(uniqueFacts.values());
        // MySQL publishes the active build only AFTER the Neo4j transaction succeeds.
        // A crash between stores may leave unreachable graph data, never a partially visible version.
        graph.publish(owner,input.documentId(),input.version(),build,facts);
        repository.save(owner,new Document(input.documentId(),input.title(),input.version(),input.text(),build,chunks,"READY"));
        if(lab!=null)lab.snapshot(owner,input.documentId(),input.version(),input.title(),facts);
        return Map.of("documentId",input.documentId(),"version",input.version(),"chunkCount",chunks.size(),"relationCount",facts.size(),"unchanged",false);
    }
    @Transactional
    public void tombstone(long owner,String id,long version) {
        settings.requireEnabled(); id(id);
        if(version<1) throw new IllegalArgumentException("删除版本必须为正整数");
        Document old=repository.lock(owner,id);
        if(old.version()==version && old.state().startsWith("DELETED")) return;
        if(version<=old.version()) throw new IllegalArgumentException("删除事件版本过期，请刷新后重试");
        repository.save(owner,new Document(id,old.title(),version,"","",List.of(),"DELETED_PENDING"));
        if(lab!=null)lab.purge(owner,id);
    }
    public Map<String,Object> cleanup(long owner,String id,long version) {
        // Called AFTER tombstone transaction commit. Retrying DELETE also retries physical cleanup.
        try { graph.delete(owner,id,version); repository.finishDeletion(owner,id,version); return Map.of("deleted",true,"cleanupPending",false); }
        catch(IllegalStateException e) { return Map.of("deleted",true,"cleanupPending",true,"message","检索权限已移除，图数据库清理失败；请重试相同删除版本"); }
    }
    public List<Map<String,Object>> pendingDeletions(long owner) {
        settings.requireEnabled();return repository.pendingDeletions(owner);
    }
    public Graph visualize(long owner) {
        return visualize(owner,List.of(),"");
    }
    public Graph visualize(long owner,List<String> documentIds,String entity) {
        return visualizeSeeds(owner,documentIds,entity,List.of());
    }
    public Graph visualizeSeeds(long owner,List<String> documentIds,String entity,List<String> seeds) {
        settings.requireEnabled();
        if(entity==null || entity.length()>120) throw new IllegalArgumentException("实体查询最长120字");
        List<Document> documents=scope(repository.list(owner),documentIds);
        Graph result=seeds.isEmpty()?graph.graph(owner,documents.stream().map(Document::buildId).toList(),entity.strip()):graph.graph(owner,documents.stream().map(Document::buildId).toList(),entity.strip(),seeds);
        Set<String> used=new HashSet<>();result.facts().forEach(f->used.add(f.chunkId()));
        List<Evidence> sources=new ArrayList<>();
        for(Document document:documents)for(Chunk c:document.chunks())if(used.contains(c.id()))
            sources.add(new Evidence("S"+(sources.size()+1),c.documentId(),c.title(),c.version(),c.id(),c.text(),sourceLocations==null?"片段"+(c.ordinal()+1):sourceLocations.resolve(owner,c)));
        return new Graph(result.facts(),result.truncated(),sources);
    }
    public Answer ask(long owner,Question input) {
        settings.requireEnabled();
        if(input==null || input.question()==null || input.question().isBlank() || input.question().length()>1000)
            throw new IllegalArgumentException("请输入1到1000字的问题");
        String mode=Objects.requireNonNullElse(input.mode(),"GRAPHRAG").toUpperCase(Locale.ROOT);
        if(!Set.of("RAG","GRAPHRAG","HYBRID").contains(mode)) throw new IllegalArgumentException("mode必须为RAG、GRAPHRAG或HYBRID");
        List<Document> docs=scope(repository.list(owner),input.documentIds());
        Map<String,Chunk> authorized=new LinkedHashMap<>();
        docs.forEach(d->d.chunks().forEach(c->authorized.put(c.id(),c)));
        List<String> builds=docs.stream().map(Document::buildId).toList();
        List<Fact> facts=mode.equals("RAG")?List.of():graph.graph(owner,builds,input.question()).facts();
        List<String> linked=List.of();
        if(lab!=null&&!mode.equals("RAG")&&!builds.isEmpty()){
            List<String> approved=lab.seeds(owner,input.question(),graph.entities(owner,builds));
            if(!approved.isEmpty()){linked=approved;facts=graph.graph(owner,builds,input.question(),approved).facts();}
        }
        if(!mode.equals("RAG") && facts.isEmpty() && !builds.isEmpty() && settings.modelConfigured()) {
            linked=model.linkEntities(input.question(),graph.entities(owner,builds));
            if(!linked.isEmpty()) facts=graph.graph(owner,builds,input.question(),linked).facts();
        }
        LinkedHashMap<String,Chunk> selected=new LinkedHashMap<>();
        for(Fact f:facts) if(authorized.containsKey(f.chunkId()) && selected.size()<8) selected.put(f.chunkId(),authorized.get(f.chunkId()));
        if(!mode.equals("GRAPHRAG")) {
            List<Chunk> recalled=rag.retrieve(owner,input.question(),List.copyOf(authorized.values()),6);
            if(recalled==null || recalled.size()>6) throw new IllegalStateException("检索器返回的片段超过约定上限");
            for(Chunk c:recalled) {
                if(c==null) throw new SecurityException("检索器返回了无效片段");
                Chunk trusted=authorized.get(c.id());
                if(trusted==null || !trusted.equals(c)) throw new SecurityException("检索器返回了无权访问或过期的片段");
                if(selected.size()<12) selected.put(c.id(),trusted);
            }
        }
        List<Evidence> sources=new ArrayList<>();
        for(Chunk c:selected.values()) sources.add(new Evidence("S"+(sources.size()+1),c.documentId(),c.title(),c.version(),c.id(),c.text(),sourceLocations==null?"片段"+(c.ordinal()+1):sourceLocations.resolve(owner,c)));
        List<Fact> citedFacts=facts.stream().filter(f->selected.containsKey(f.chunkId())).limit(40).toList();
        String retrieval=mode.equals("GRAPHRAG")?"实体名称匹配＋两跳图谱检索":mode.equals("RAG")?rag.name():"两跳图谱＋"+rag.name();
        if(!linked.isEmpty()) retrieval+="；模型定位实体："+String.join("、",linked);
        if(sources.isEmpty()) return new Answer(KnowledgeModel.INSUFFICIENT,mode,retrieval,List.of(),List.of(),"");
        String answer=model.answer(input.question(),sources,citedFacts);
        try { validateCitations(answer,sources); }
        catch(IllegalStateException invalidCitation) {
            answer=model.answer(input.question()+"\n输出校验要求：有依据的事实必须标注给定来源编号；如果依据不足，请只输出固定句："+KnowledgeModel.INSUFFICIENT,sources,citedFacts);
            validateCitations(answer,sources);
        }
        // Do not return an answer based on a file that was updated or deleted while the model was running.
        Map<String,String> currentBuilds=new HashMap<>();
        repository.list(owner).forEach(d->currentBuilds.put(d.documentId(),d.buildId()));
        for(Document d:docs) if(selected.values().stream().anyMatch(c->c.documentId().equals(d.documentId()))
                && !Objects.equals(d.buildId(),currentBuilds.get(d.documentId())))
            throw new IllegalStateException("回答生成期间资料已更新或删除，请重新提问以使用最新版本");
        return new Answer(answer,mode,retrieval,sources,citedFacts,settings.getModel());
    }
    static List<Document> scope(List<Document> docs,List<String> ids) {
        if(ids==null || ids.isEmpty()) return docs;
        if(ids.size()>50) throw new IllegalArgumentException("一次最多选择50份资料");
        Set<String> requested=new HashSet<>(ids);
        requested.forEach(KnowledgeService::id);
        List<Document> selected=docs.stream().filter(d->requested.contains(d.documentId())).toList();
        if(selected.size()!=requested.size()) throw new SecurityException("所选资料不存在、已更新删除或无权访问，请刷新资料列表");
        return selected;
    }
    public Map<String,Object> probeModel() {
        settings.requireEnabled();
        model.probe();
        return Map.of("connected",true,"model",settings.getModel());
    }
    static void validateCitations(String answer,List<Evidence> sources) {
        if(KnowledgeModel.INSUFFICIENT.equals(answer))return;
        Set<String> allowed=new HashSet<>(); sources.forEach(s->allowed.add(s.id()));
        Matcher refs=Pattern.compile("\\[(S\\d+)\\]").matcher(answer);
        boolean any=false;
        while(refs.find()) { any=true; if(!allowed.contains(refs.group(1))) throw new IllegalStateException("模型引用了不存在的来源，本次回答未发布"); }
        if(!any) throw new IllegalStateException("模型回答缺少来源编号，请重试");
    }
    static void validate(DocumentInput in) {
        if(in==null) throw new IllegalArgumentException("文件内容不能为空");
        id(in.documentId());
        if(in.title()==null || in.title().isBlank() || in.title().length()>200) throw new IllegalArgumentException("标题应为1到200字");
        if(in.version()<1) throw new IllegalArgumentException("文件版本必须为正整数");
        if(in.text()==null || in.text().isBlank() || in.text().length()>12000) throw new IllegalArgumentException("文本应为1到12000字，较长文件请由任务二拆分");
    }
    private static void id(String id) { if(id==null || !id.matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("文件编号仅允许1到64位字母、数字、下划线与短横线"); }
    static List<Chunk> chunks(DocumentInput input,String build) {
        List<Chunk> chunks=new ArrayList<>();
        for(int start=0,ordinal=0; start<input.text().length();ordinal++) {
            int end=Math.min(start+1800,input.text().length());
            // Do not split a UTF-16 surrogate pair at a boundary.
            if(end<input.text().length() && Character.isHighSurrogate(input.text().charAt(end-1))) end--;
            chunks.add(new Chunk(build+":"+ordinal,input.documentId(),input.title(),input.version(),ordinal,input.text().substring(start,end)));
            if(end==input.text().length()) break;
            start=end-150;
            if(Character.isLowSurrogate(input.text().charAt(start))) start--;
        }
        return chunks;
    }
}
