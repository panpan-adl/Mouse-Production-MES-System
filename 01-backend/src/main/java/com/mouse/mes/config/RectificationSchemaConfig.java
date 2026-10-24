package com.mouse.mes.config;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
@Component
public class RectificationSchemaConfig implements CommandLineRunner {
 private final DataSource source;
 public RectificationSchemaConfig(DataSource source){this.source=source;}
 public void run(String... args){new ResourceDatabasePopulator(new ClassPathResource("rectification.sql")).execute(source);
 var db=new org.springframework.jdbc.core.JdbcTemplate(source);
 if(db.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='t_material_request' AND column_name='required_date'",Integer.class)==0)db.execute("ALTER TABLE t_material_request ADD COLUMN required_date DATE NULL");}
}
