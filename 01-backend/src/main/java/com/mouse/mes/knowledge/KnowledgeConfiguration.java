package com.mouse.mes.knowledge;

import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import com.mouse.mes.knowledge.rag.AuthorizedChunkIndex;

@Configuration
public class KnowledgeConfiguration {
    @Bean
    @ConditionalOnMissingBean(RagRetriever.class)
    public RagRetriever knowledgeRagRetriever(ObjectProvider<AuthorizedChunkIndex> indexes) {
        AuthorizedChunkIndex index=indexes.getIfAvailable();
        return index==null?new LexicalRagRetriever():new VectorRagRetriever(index);
    }
}
