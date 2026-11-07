package com.mouse.mes.knowledge;

import java.util.List;
import com.mouse.mes.knowledge.KnowledgeTypes.Chunk;

/** Task two extension: return at most limit of the authorized, current-version chunks. */
public interface RagRetriever {
    String name();
    List<Chunk> retrieve(long owner, String question, List<Chunk> authorizedChunks, int limit);
}
