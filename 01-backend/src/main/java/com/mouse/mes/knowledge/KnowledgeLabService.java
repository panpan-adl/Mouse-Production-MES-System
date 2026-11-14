package com.mouse.mes.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.mouse.mes.knowledge.KnowledgeTypes.*;

@Service
public class KnowledgeLabService {
    private final KnowledgeService knowledge;
    private final KnowledgeRepository documents;
    private final KnowledgeLabRepository lab;
    private final Neo4jKnowledgeStore graph;
    private final KnowledgeModel model;
    private final KnowledgeSettings settings;
    public KnowledgeLabService(KnowledgeService knowledge,KnowledgeRepository documents,KnowledgeLabRepository lab,Neo4jKnowledgeStore graph,KnowledgeModel model,KnowledgeSettings settings){this.knowledge=knowledge;this.documents=documents;this.lab=lab;this.graph=graph;this.model=model;this.settings=settings;}
    private List<Document> scoped(long owner,List<String> ids){settings.requireEnabled();return KnowledgeService.scope(documents.list(owner),ids);}
    private Document current(long owner,String id){return scoped(owner,List.of(id)).get(0);}
    private Map<String,String> builds(List<Document> docs){Map<String,String> out=new TreeMap<>();docs.forEach(d->out.put(d.documentId(),d.buildId()));return out;}
    public List<Map<String,Object>> versions(long owner,String id){current(owner,id);return lab.versions(owner,id);}
    public Map<String,Object> diff(long owner,String id,long before,long after){
        current(owner,id);if(before<1||after<=before)throw new IllegalArgumentException("请选择从旧到新的两个版本");
        return difference(before,after,lab.facts(owner,id,before),lab.facts(owner,id,after));
    }
    static String key(Fact f){return String.join("\u0000",f.source(),f.relation(),f.target());}
    static Map<String,Object> difference(long before,long after,List<Fact> old,List<Fact> fresh){
        Map<String,Fact> a=new LinkedHashMap<>(),b=new LinkedHashMap<>();old.forEach(f->a.putIfAbsent(key(f),f));fresh.forEach(f->b.putIfAbsent(key(f),f));
        return Map.of("before",before,"after",after,"added",b.entrySet().stream().filter(e->!a.containsKey(e.getKey())).map(Map.Entry::getValue).toList(),"removed",a.entrySet().stream().filter(e->!b.containsKey(e.getKey())).map(Map.Entry::getValue).toList(),"retained",b.entrySet().stream().filter(e->a.containsKey(e.getKey())).map(Map.Entry::getValue).toList(),"relationIdentity","source+relation+target");
    }
    public Map<String,Object> compare(long owner,Question input){
        if(input==null)throw new IllegalArgumentException("请输入问题");
        List<Document> docs=scoped(owner,input.documentIds());Map<String,String> before=builds(docs);
        List<Map<String,Object>> results=new ArrayList<>();
        for(String mode:List.of("RAG","GRAPHRAG","HYBRID")){
            long start=System.nanoTime();Map<String,Object> row=new LinkedHashMap<>();row.put("mode",mode);
            try{row.put("result",knowledge.ask(owner,new Question(input.question(),mode,new ArrayList<>(before.keySet()))));row.put("success",true);}
            catch(IllegalArgumentException|SecurityException e){throw e;}
            catch(IllegalStateException e){row.put("success",false);row.put("error",e.getMessage());}
            row.put("milliseconds",(System.nanoTime()-start)/1_000_000);results.add(row);
        }
        if(!before.equals(builds(scoped(owner,input.documentIds()))))throw new IllegalStateException("对比期间资料已变化，结果未发布，请重新对比");
        return Map.of("question",input.question(),"versions",docs.stream().map(d->Map.of("documentId",d.documentId(),"version",d.version())).toList(),"results",results);
    }
    public record DialogueInput(String question,String mode,List<String> documentIds,String contextId){}
    public Map<String,Object> dialogue(long owner,DialogueInput input){
        if(input==null||input.question()==null||input.question().isBlank()||input.question().length()>1000)throw new IllegalArgumentException("请输入1到1000字的问题");
        var docs=scoped(owner,input.documentIds());var active=builds(docs);String resolved=input.question();
        if(input.contextId()!=null&&!input.contextId().isBlank()){
            var previous=lab.context(owner,input.contextId());if(!active.equals(lab.builds(previous)))throw new IllegalStateException("追问资料范围或版本已变化，请开启新话题");
            resolved=knowledge.resolveFollowup(owner,input.question(),previous.get("question").toString(),previous.get("answer").toString());
        }
        Answer answer=knowledge.ask(owner,new Question(resolved,input.mode(),new ArrayList<>(active.keySet())));
        if(!active.equals(builds(scoped(owner,input.documentIds()))))throw new IllegalStateException("追问期间资料已变化，请重新提问");
        String id=lab.remember(owner,resolved,answer,active);
        return Map.of("result",answer,"contextId",id,"resolvedQuestion",resolved);
    }
    private List<String> names(long owner,List<Document> docs){return graph.entities(owner,docs.stream().map(Document::buildId).toList());}
    public Map<String,Object> quality(long owner,List<String> ids){
        List<String> names=names(owner,scoped(owner,ids));var types=lab.types(owner);var aliases=lab.aliases(owner);
        List<Map<String,String>> nodes=names.stream().map(n->Map.of("name",n,"type",types.getOrDefault(n,"OTHER"),"suggestedType",suggestType(n))).toList();
        List<Map<String,String>> suggestions=new ArrayList<>();
        for(int i=0;i<names.size();i++)for(int j=i+1;j<names.size()&&suggestions.size()<30;j++){
            String a=names.get(i),b=names.get(j);
            if(!a.equals(b)&&normalized(a).equals(normalized(b))&&!normalized(a).isBlank()&&!aliases.containsKey(a)&&!aliases.containsKey(b))suggestions.add(Map.of("alias",a,"canonical",b,"reason","名称规范化相同，型号数字保留；仍需人工确认"));
        }
        return Map.of("entities",nodes,"aliases",aliases.entrySet().stream().filter(e->names.contains(e.getKey())&&names.contains(e.getValue())).map(e->Map.of("alias",e.getKey(),"canonical",e.getValue())).toList(),"suggestions",suggestions,"candidateLimit",300);
    }
    static String normalized(String n){return n.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]","").replace("设备","机").replace("机器","机");}
    static String suggestType(String name){
        if(name.matches("(?i).*(工序|工艺|process|step).*"))return "PROCESS";
        if(name.matches("(?i).*(点检|检验|检测|check|inspection).*"))return "INSPECTION";
        if(name.matches("(?i).*(机|设备|robot|equipment).*"))return "EQUIPMENT";
        if(name.matches("(?i).*(鼠标|产品|mouse|product).*"))return "PRODUCT";
        if(name.matches(".*(师傅|工程师|负责人).*"))return "PERSON";return "OTHER";
    }
    public record Review(String name,String type,String canonical){}
    @Transactional public void review(long owner,Review input){
        settings.requireEnabled();if(input==null||input.name()==null)throw new IllegalArgumentException("请选择实体");
        var names=names(owner,scoped(owner,null));if(!names.contains(input.name()))throw new SecurityException("实体不存在或无权访问");
        if(input.type()!=null){
            if(!Set.of("EQUIPMENT","PROCESS","PRODUCT","PERSON","INSPECTION","OTHER").contains(input.type()))throw new IllegalArgumentException("实体类型无效");
            var confirmed=lab.types(owner);var existingAliases=lab.aliases(owner);
            for(var entry:existingAliases.entrySet()){
                String other=entry.getKey().equals(input.name())?entry.getValue():entry.getValue().equals(input.name())?entry.getKey():null;
                if(other!=null&&!input.type().equals("OTHER")&&!confirmed.getOrDefault(other,"OTHER").equals("OTHER")&&!input.type().equals(confirmed.get(other)))throw new IllegalArgumentException("该类型与已确认别名冲突，请先撤销别名");
            }
            lab.type(owner,input.name(),input.type());
        }
        if(input.canonical()!=null){
            if(input.canonical().isBlank()){lab.removeAlias(owner,input.name());return;}
            if(input.name().equals(input.canonical())||!names.contains(input.canonical()))throw new IllegalArgumentException("请选择另一个现有实体作为规范名称");
            var aliases=lab.aliases(owner);
            if(aliases.containsKey(input.canonical())||aliases.containsValue(input.name()))throw new IllegalArgumentException("不能创建别名链或循环，请先撤销原有别名");
            var type=lab.types(owner);String a=type.getOrDefault(input.name(),"OTHER"),b=type.getOrDefault(input.canonical(),"OTHER");
            if(!a.equals("OTHER")&&!b.equals("OTHER")&&!a.equals(b))throw new IllegalArgumentException("不同实体类型不能合并称呼");
            String digitsA=input.name().replaceAll("\\D", ""),digitsB=input.canonical().replaceAll("\\D", "");
            if(!digitsA.equals(digitsB))throw new IllegalArgumentException("型号数字不同，不能确认为同一实体");
            lab.alias(owner,input.name(),input.canonical());
        }
    }
    public Map<String,Object> association(long owner,String entity,List<String> ids){
        var docs=scoped(owner,ids);var names=names(owner,docs);
        if(entity==null||!names.contains(entity))throw new IllegalArgumentException("请选择当前资料中的实体");
        // Approved aliases expand the seed set; never rewrite stored provenance.
        var approved=lab.seeds(owner,entity,names);
        Graph result=knowledge.visualizeSeeds(owner,ids,entity,approved);
        var types=lab.types(owner);Map<String,List<String>> groups=new LinkedHashMap<>();
        for(String type:List.of("EQUIPMENT","PROCESS","PRODUCT","PERSON","INSPECTION","OTHER"))groups.put(type,new ArrayList<>());
        result.facts().stream().flatMap(f->java.util.stream.Stream.of(f.source(),f.target())).distinct().filter(n->!n.equals(entity)).forEach(n->groups.get(types.getOrDefault(n,suggestType(n))).add(n));
        return Map.of("entity",entity,"groups",groups,"graph",result,"notice","资料中的两跳关联，供工艺和维护核对，不代表实时生产状态");
    }
}
