package com.mouse.mes.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
import static com.mouse.mes.knowledge.KnowledgeTypes.*;

/** Additive owner-scoped course features. Snapshots join the existing publication transaction. */
@Repository
public class KnowledgeLabRepository {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    public KnowledgeLabRepository(JdbcTemplate db,ObjectMapper json){
        this.db=db;this.json=json;
        db.execute("CREATE TABLE IF NOT EXISTS mm_kg_snapshot(owner BIGINT NOT NULL,document_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,version BIGINT NOT NULL,title VARCHAR(200) NOT NULL,facts_json MEDIUMTEXT NOT NULL,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(owner,document_id,version)) ENGINE=InnoDB");
        db.execute("CREATE TABLE IF NOT EXISTS mm_kg_entity_review(owner BIGINT NOT NULL,name VARCHAR(120) COLLATE utf8mb4_bin NOT NULL,entity_type VARCHAR(24) NOT NULL,PRIMARY KEY(owner,name)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        db.execute("CREATE TABLE IF NOT EXISTS mm_kg_alias(owner BIGINT NOT NULL,alias_name VARCHAR(120) COLLATE utf8mb4_bin NOT NULL,canonical VARCHAR(120) COLLATE utf8mb4_bin NOT NULL,PRIMARY KEY(owner,alias_name)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        db.execute("CREATE TABLE IF NOT EXISTS mm_kg_dialogue(owner BIGINT NOT NULL,context_id VARCHAR(36) NOT NULL,question VARCHAR(1000) NOT NULL,answer MEDIUMTEXT NOT NULL,builds_json MEDIUMTEXT NOT NULL,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(owner,context_id)) ENGINE=InnoDB");
    }
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("知识实验数据编码失败");}}
    public void snapshot(long owner,String id,long version,String title,List<Fact> facts){db.update("INSERT INTO mm_kg_snapshot(owner,document_id,version,title,facts_json) VALUES(?,?,?,?,?)",owner,id,version,title,encode(facts));}
    public void purge(long owner,String id){
        db.update("DELETE FROM mm_kg_snapshot WHERE owner=? AND document_id=?",owner,id);
        // Context may contain an earlier answer to this document. Revoke the owner's contexts on deletion.
        db.update("DELETE FROM mm_kg_dialogue WHERE owner=?",owner);
    }
    public List<Map<String,Object>> versions(long owner,String id){return db.queryForList("SELECT version,title,created_at AS createdAt FROM mm_kg_snapshot WHERE owner=? AND document_id=? ORDER BY version DESC",owner,id);}
    public List<Fact> facts(long owner,String id,long version){
        var rows=db.queryForList("SELECT facts_json FROM mm_kg_snapshot WHERE owner=? AND document_id=? AND version=?",owner,id,version);
        if(rows.isEmpty())throw new IllegalArgumentException("该版本没有图谱快照，请选择功能启用后发布的版本");
        try{return json.readValue(rows.get(0).get("facts_json").toString(),new TypeReference<List<Fact>>(){});}catch(Exception e){throw new IllegalStateException("图谱快照读取失败");}
    }
    public Map<String,String> aliases(long owner){return rows("SELECT alias_name,canonical FROM mm_kg_alias WHERE owner=?",owner,"alias_name","canonical");}
    public Map<String,String> types(long owner){return rows("SELECT name,entity_type FROM mm_kg_entity_review WHERE owner=?",owner,"name","entity_type");}
    private Map<String,String> rows(String sql,long owner,String key,String value){Map<String,String> out=new LinkedHashMap<>();for(var r:db.queryForList(sql,owner))out.put(r.get(key).toString(),r.get(value).toString());return out;}
    public void type(long owner,String name,String type){db.update("INSERT INTO mm_kg_entity_review(owner,name,entity_type) VALUES(?,?,?) ON DUPLICATE KEY UPDATE entity_type=VALUES(entity_type)",owner,name,type);}
    public void alias(long owner,String alias,String canonical){db.update("INSERT INTO mm_kg_alias(owner,alias_name,canonical) VALUES(?,?,?) ON DUPLICATE KEY UPDATE canonical=VALUES(canonical)",owner,alias,canonical);}
    public void removeAlias(long owner,String alias){db.update("DELETE FROM mm_kg_alias WHERE owner=? AND alias_name=?",owner,alias);}
    public List<String> seeds(long owner,String question,List<String> candidates){
        Set<String> allowed=new HashSet<>(candidates),result=new LinkedHashSet<>();
        aliases(owner).forEach((alias,canonical)->{
            if(allowed.contains(alias)&&allowed.contains(canonical)&&(question.toLowerCase(Locale.ROOT).contains(alias.toLowerCase(Locale.ROOT))||question.toLowerCase(Locale.ROOT).contains(canonical.toLowerCase(Locale.ROOT)))){result.add(alias);result.add(canonical);}
        });return result.stream().limit(8).toList();
    }
    public String remember(long owner,String question,Answer answer,Map<String,String> builds){
        db.update("DELETE FROM mm_kg_dialogue WHERE owner=? AND created_at<TIMESTAMPADD(HOUR,-2,CURRENT_TIMESTAMP)",owner);
        var stale=db.queryForList("SELECT context_id FROM mm_kg_dialogue WHERE owner=? ORDER BY created_at DESC LIMIT 100 OFFSET 29",owner);
        for(var row:stale)db.update("DELETE FROM mm_kg_dialogue WHERE owner=? AND context_id=?",owner,row.get("context_id"));
        String id=UUID.randomUUID().toString();db.update("INSERT INTO mm_kg_dialogue(owner,context_id,question,answer,builds_json) VALUES(?,?,?,?,?)",owner,id,question,answer.answer(),encode(builds));return id;
    }
    public Map<String,Object> context(long owner,String id){
        if(id==null||!id.matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("追问会话编号无效");
        var rows=db.queryForList("SELECT question,answer,builds_json FROM mm_kg_dialogue WHERE owner=? AND context_id=? AND created_at>=TIMESTAMPADD(HOUR,-2,CURRENT_TIMESTAMP)",owner,id);
        if(rows.isEmpty())throw new SecurityException("追问已过期或无权访问，请开启新话题");return rows.get(0);
    }
    public Map<String,String> builds(Map<String,Object> row){try{return json.readValue(row.get("builds_json").toString(),new TypeReference<Map<String,String>>(){});}catch(Exception e){throw new IllegalStateException("追问资料范围读取失败");}}
}
