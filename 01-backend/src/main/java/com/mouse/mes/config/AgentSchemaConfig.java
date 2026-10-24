package com.mouse.mes.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AgentSchemaConfig implements CommandLineRunner {
    private final JdbcTemplate db;
    public AgentSchemaConfig(JdbcTemplate db){this.db=db;}
    @Override public void run(String...args){
        db.execute("CREATE TABLE IF NOT EXISTS mm_agent_run (id VARCHAR(36) PRIMARY KEY,user_id BIGINT NOT NULL,skill_id VARCHAR(32) NOT NULL,question VARCHAR(1200) NOT NULL,status VARCHAR(24) NOT NULL,payload LONGTEXT NOT NULL,result LONGTEXT NOT NULL,created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),INDEX idx_agent_user(user_id,created_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }
}
