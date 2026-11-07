package com.mouse.mes.knowledge.rag;

import com.mouse.mes.knowledge.*;
import org.springframework.stereotype.Service;
import java.util.*;

/** Called through the injected KnowledgeService proxy, joining the file publication transaction. */
@Service
public class RagKnowledgeBridge {
    private final KnowledgeService knowledge;
    private final KnowledgeRepository repository;
    public RagKnowledgeBridge(KnowledgeService knowledge,KnowledgeRepository repository){this.knowledge=knowledge;this.repository=repository;}
    public KnowledgeTypes.Document publish(long owner,String id,int version,String title,String text) {
        knowledge.upsert(owner,new KnowledgeTypes.DocumentInput(id,title,version,text));
        return repository.list(owner).stream().filter(d->d.documentId().equals(id)&&d.version()==version).findFirst()
                .orElseThrow(()->new IllegalStateException("图谱版本尚未发布"));
    }
    public boolean current(long owner,String id,int version) {
        return repository.list(owner).stream().anyMatch(d->d.documentId().equals(id)&&d.version()==version);
    }
    public Optional<KnowledgeTypes.Document> original(long owner,String id){return repository.list(owner).stream().filter(d->d.documentId().equals(id)).findFirst();}
    public void tombstone(long owner,String id,int version){knowledge.tombstone(owner,id,version);}
    public Map<String,Object> cleanup(long owner,String id,int version){return knowledge.cleanup(owner,id,version);}
}
