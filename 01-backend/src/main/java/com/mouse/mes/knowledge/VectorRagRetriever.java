package com.mouse.mes.knowledge;

import com.mouse.mes.knowledge.rag.*;
import java.util.List;

/** Real vector retrieval over the SAME authorized Chunk objects used by GraphRAG. */
public class VectorRagRetriever implements RagRetriever {
    private final AuthorizedChunkIndex index;
    public VectorRagRetriever(AuthorizedChunkIndex index){this.index=index;}
    @Override public String name(){return index.name();}
    @Override public List<KnowledgeTypes.Chunk> retrieve(long owner,String question,List<KnowledgeTypes.Chunk> chunks,int limit){
        try {
            return index.retrieve(owner,question,chunks,limit,c->new RagTypes.Chunk(c.id(),c.documentId(),Math.toIntExact(c.version()),c.title(),c.text(),"片段"+(c.ordinal()+1)));
        } catch(IllegalArgumentException|IllegalStateException e){throw e;}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("向量检索已中断");}
        catch(Exception e){throw new IllegalStateException("向量检索失败，请检查嵌入模型配置及文件索引状态");}
    }
}
