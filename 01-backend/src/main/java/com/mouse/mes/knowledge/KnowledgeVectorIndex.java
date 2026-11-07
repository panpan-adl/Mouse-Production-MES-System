package com.mouse.mes.knowledge;

/** A version is only published after both graph and vector writes have succeeded. */
public interface KnowledgeVectorIndex {
    void prepare(long owner, KnowledgeTypes.Document document);
    void remove(long owner, String documentId, long version);
    default String location(long owner,KnowledgeTypes.Chunk chunk){return "片段"+(chunk.ordinal()+1);}
}
