package com.mouse.mes.knowledge;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;

@Component
@ConditionalOnProperty(name="mes.knowledge.enabled",havingValue="true")
public class KnowledgeSchema implements CommandLineRunner {
    private final DataSource dataSource;
    public KnowledgeSchema(DataSource dataSource) { this.dataSource=dataSource; }
    @Override public void run(String... args) {
        new ResourceDatabasePopulator(new ClassPathResource("sql/knowledge_graph.sql")).execute(dataSource);
    }
}
