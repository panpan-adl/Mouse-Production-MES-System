package com.mouse.mes.knowledge.rag;

import com.mouse.mes.knowledge.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;
import java.util.List;

@Component
@Primary
public class VectorRagRetriever implements RagRetriever, KnowledgeVectorIndex {
    private final AuthorizedChunkIndex index;
    public VectorRagRetriever(AuthorizedChunkIndex index){this.index=index;}
    private RagTypes.Chunk view(KnowledgeTypes.Chunk c){return new RagTypes.Chunk(c.id(),c.documentId(),Math.toIntExact(c.version()),c.title(),c.text(),"片段"+(c.ordinal()+1));}
    @Override public String name(){return index.name();}
    @Override public List<KnowledgeTypes.Chunk> retrieve(long owner,String question,List<KnowledgeTypes.Chunk> authorizedChunks,int limit) {
        try{return index.retrieve(owner,question,authorizedChunks,limit,this::view);}
        catch(Exception e){throw new IllegalStateException("向量检索失败："+e.getMessage(),e);}
    }
    @Override public void prepare(long owner,KnowledgeTypes.Document doc) {
        try{index.prepareDocument(owner,doc.chunks(),this::view);}
        catch(Exception e){throw new IllegalStateException("向量构建失败，新版本尚未发布："+e.getMessage(),e);}
    }
    @Override public void remove(long owner,String id,long version){index.deleteDocument(owner,id,version);}
    @Override public String location(long owner,KnowledgeTypes.Chunk c){return index.location(owner,c.id(),"片段"+(c.ordinal()+1));}
}
