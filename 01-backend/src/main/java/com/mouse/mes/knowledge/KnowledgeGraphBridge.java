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
    @Override @Transactional public Published publish(long owner,String id,int version,String title,String text){
        knowledge.upsert(owner,new KnowledgeTypes.DocumentInput(id,title,version,text));
        var doc=repository.list(owner).stream().filter(d->d.documentId().equals(id)&&d.version()==version).findFirst().orElseThrow(()->new IllegalStateException("图谱版本尚未发布"));
        return new Published(doc.buildId(),doc.chunks().stream().map(c->new RagTypes.Chunk(c.id(),c.documentId(),Math.toIntExact(c.version()),c.title(),c.text(),"片段"+(c.ordinal()+1))).toList());
    }
    @Override @Transactional public void revoke(long owner,String id,int version){
        knowledge.tombstone(owner,id,version);
    }
    @Override public boolean cleanup(long owner,String id,int version){return Boolean.TRUE.equals(knowledge.cleanup(owner,id,version).get("cleanupPending"));}
}
