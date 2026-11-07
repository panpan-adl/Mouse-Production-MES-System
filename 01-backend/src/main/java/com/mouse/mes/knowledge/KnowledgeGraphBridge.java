package com.mouse.mes.knowledge;

import com.mouse.mes.knowledge.rag.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
@ConditionalOnProperty(name="mes.knowledge.enabled",havingValue="true")
public class KnowledgeGraphBridge implements RagGraphBridge {
    private final KnowledgeService knowledge;
    private final KnowledgeRepository repository;
    private final AuthorizedChunkIndex index;
    public KnowledgeGraphBridge(KnowledgeService knowledge,KnowledgeRepository repository,AuthorizedChunkIndex index){this.knowledge=knowledge;this.repository=repository;this.index=index;}
    private List<String> chunkIds(long owner,String id){return repository.list(owner).stream().filter(d->d.documentId().equals(id)).flatMap(d->d.chunks().stream()).map(KnowledgeTypes.Chunk::id).toList();}
    @Override @Transactional public void publish(long owner,String id,int version,String title,String text){
        List<String> previous=chunkIds(owner,id);
        knowledge.upsert(owner,new KnowledgeTypes.DocumentInput(id,title,version,text));
        index.evict(owner,previous);
    }
    @Override @Transactional public void revoke(long owner,String id,int version){
        List<String> previous=chunkIds(owner,id);knowledge.tombstone(owner,id,version);index.evict(owner,previous);
    }
    @Override public boolean cleanup(long owner,String id,int version){return Boolean.TRUE.equals(knowledge.cleanup(owner,id,version).get("cleanupPending"));}
}
