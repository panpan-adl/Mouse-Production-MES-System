package com.mouse.mes.knowledge;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mes.knowledge")
public class KnowledgeSettings {
    private boolean enabled;
    private String neo4jUrl = "http://127.0.0.1:7474";
    private String neo4jDatabase = "neo4j";
    private String neo4jUser = "neo4j";
    private String neo4jPassword = "";
    private String modelBaseUrl = "";
    private String model = "";
    private String modelApiKey = "";
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public String getNeo4jUrl() { return neo4jUrl; }
    public void setNeo4jUrl(String value) { neo4jUrl = value; }
    public String getNeo4jDatabase() { return neo4jDatabase; }
    public void setNeo4jDatabase(String value) { neo4jDatabase = value; }
    public String getNeo4jUser() { return neo4jUser; }
    public void setNeo4jUser(String value) { neo4jUser = value; }
    public String getNeo4jPassword() { return neo4jPassword; }
    public void setNeo4jPassword(String value) { neo4jPassword = value; }
    public String getModelBaseUrl() { return modelBaseUrl; }
    public void setModelBaseUrl(String value) { modelBaseUrl = value; }
    public String getModel() { return model; }
    public void setModel(String value) { model = value; }
    public String getModelApiKey() { return modelApiKey; }
    public void setModelApiKey(String value) { modelApiKey = value; }
    public boolean modelConfigured() { return !modelBaseUrl.isBlank() && !model.isBlank() && !modelApiKey.isBlank(); }
    public void requireEnabled() {
        if (!enabled) throw new IllegalStateException("知识库模块尚未启用，请配置 MES_KNOWLEDGE_ENABLED");
    }
}
