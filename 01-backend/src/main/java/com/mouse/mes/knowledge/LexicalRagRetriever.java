package com.mouse.mes.knowledge;

import java.util.*;
import com.mouse.mes.knowledge.KnowledgeTypes.Chunk;

/** Explicit baseline until task two supplies vector retrieval; never presented as embeddings. */
public class LexicalRagRetriever implements RagRetriever {
    @Override public String name() { return "关键词基线（尚未接入任务二向量检索）"; }
    @Override public List<Chunk> retrieve(long owner,String question,List<Chunk> chunks,int limit) {
        Set<String> terms = new HashSet<>();
        for (String word: question.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_-]+")) {
            if(word.isBlank()) continue;
            if(word.matches(".*[\\p{IsHan}].*")) {
                for(int i=0;i<word.length()-1;i++) terms.add(word.substring(i,i+2));
            } else if(word.length()>1) terms.add(word);
        }
        Map<Chunk,Long> scores=new HashMap<>();
        for(Chunk c:chunks) scores.put(c,terms.stream().filter(c.text().toLowerCase(Locale.ROOT)::contains).count());
        return chunks.stream().filter(c->scores.get(c)>0).sorted(Comparator.<Chunk>comparingLong(scores::get).reversed().thenComparing(Chunk::id)).limit(limit).toList();
    }
}
