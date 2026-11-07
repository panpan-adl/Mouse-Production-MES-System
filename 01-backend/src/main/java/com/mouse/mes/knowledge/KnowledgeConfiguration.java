package com.mouse.mes.knowledge;

import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

@Configuration
public class KnowledgeConfiguration {
    @Bean
    @ConditionalOnMissingBean(RagRetriever.class)
    public RagRetriever lexicalRagRetriever() { return new LexicalRagRetriever(); }
}
