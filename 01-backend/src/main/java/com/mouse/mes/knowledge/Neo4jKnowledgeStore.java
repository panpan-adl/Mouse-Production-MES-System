package com.mouse.mes.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static com.mouse.mes.knowledge.KnowledgeTypes.*;

/** Neo4j Query API, available in Neo4j 5.26 LTS. All data is parameterized. */
@Service
public class Neo4jKnowledgeStore {
    private final KnowledgeSettings settings;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public Neo4jKnowledgeStore(KnowledgeSettings settings, ObjectMapper json) { this.settings = settings; this.json = json; }
    public void publish(long owner, String document, long version, String build, List<Fact> facts) {
        // Unique entity IDs are owner-scoped; relationships are immutable, build-scoped evidence.
        query("CREATE CONSTRAINT mm_kg_entity IF NOT EXISTS FOR (e:MMKGEntity) REQUIRE e.key IS UNIQUE", Map.of());
        List<Map<String,Object>> rows = new ArrayList<>();
        for (Fact f : facts) rows.add(Map.of("source",f.source(),"target",f.target(),"relation",f.relation(),"quote",f.quote(),"chunkId",f.chunkId()));
        query("UNWIND $rows AS row MERGE (a:MMKGEntity {key: toString($owner) + ':' + row.source}) SET a.name=row.source, a.owner=$owner "
                + "MERGE (b:MMKGEntity {key: toString($owner) + ':' + row.target}) SET b.name=row.target, b.owner=$owner "
                + "CREATE (a)-[:MMKG_REL {owner:$owner, documentId:$document, version:$version, buildId:$build, chunkId:row.chunkId, relation:row.relation, quote:row.quote}]->(b)",
                Map.of("rows",rows,"owner",owner,"document",document,"version",version,"build",build));
    }
    public Graph graph(long owner, List<String> builds, String question) {
        return graph(owner,builds,question,List.of());
    }
    public Graph graph(long owner,List<String> builds,String question,List<String> seeds) {
        if (builds.isEmpty()) return new Graph(List.of(), false);
        String statement;
        if (question.isBlank()) {
            statement = "MATCH (a:MMKGEntity)-[r:MMKG_REL]->(b:MMKGEntity) WHERE r.owner=$owner AND r.buildId IN $builds ";
        } else {
            statement = "MATCH p=(seed:MMKGEntity)-[:MMKG_REL*1..2]-(neighbor:MMKGEntity) "
                    + "WHERE seed.owner=$owner AND (seed.name IN $seeds OR toLower($question) CONTAINS toLower(seed.name) OR toLower(seed.name) CONTAINS toLower($question)) "
                    + "AND all(edge IN relationships(p) WHERE edge.owner=$owner AND edge.buildId IN $builds) "
                    + "UNWIND relationships(p) AS r WITH DISTINCT r WITH r, startNode(r) AS a, endNode(r) AS b ";
        }
        JsonNode values = query(statement + "RETURN a.name, r.relation, b.name, r.quote, r.chunkId ORDER BY r.chunkId, a.name, b.name LIMIT 151",
                Map.of("owner",owner,"builds",builds,"question",question,"seeds",seeds));
        List<Fact> facts = new ArrayList<>();
        for (JsonNode row : values) facts.add(new Fact(row.path(0).asText(),row.path(1).asText(),row.path(2).asText(),row.path(3).asText(),row.path(4).asText()));
        return new Graph(facts.stream().limit(150).toList(), facts.size()>150);
    }
    public List<String> entities(long owner,List<String> builds) {
        if(builds.isEmpty()) return List.of();
        JsonNode rows=query("MATCH (a:MMKGEntity)-[r:MMKG_REL]->(b:MMKGEntity) WHERE r.owner=$owner AND r.buildId IN $builds "
                +"UNWIND [a.name,b.name] AS name RETURN DISTINCT name ORDER BY name LIMIT 300",Map.of("owner",owner,"builds",builds));
        List<String> result=new ArrayList<>();for(JsonNode row:rows)result.add(row.path(0).asText());return result;
    }
    public void delete(long owner, String document, long version) {
        query("MATCH ()-[r:MMKG_REL]->() WHERE r.owner=$owner AND r.documentId=$document AND r.version<=$version DELETE r",Map.of("owner",owner,"document",document,"version",version));
        query("MATCH (e:MMKGEntity) WHERE e.owner=$owner AND NOT (e)-[:MMKG_REL]-() DELETE e",Map.of("owner",owner));
    }
    public boolean ping() { query("RETURN 1", Map.of()); return true; }
    JsonNode query(String statement, Map<String,Object> parameters) {
        if (settings.getNeo4jPassword().isBlank()) throw new IllegalStateException("尚未配置 Neo4j 密码");
        if (!settings.getNeo4jDatabase().matches("[A-Za-z0-9_-]+")) throw new IllegalStateException("Neo4j 数据库名无效");
        try {
            URI uri = URI.create(settings.getNeo4jUrl().replaceAll("/+$", "") + "/db/" + settings.getNeo4jDatabase() + "/query/v2");
            if (!Set.of("http","https").contains(uri.getScheme()) || uri.getHost()==null) throw new IllegalStateException("Neo4j 地址无效");
            String credentials = Base64.getEncoder().encodeToString((settings.getNeo4jUser()+":"+settings.getNeo4jPassword()).getBytes(StandardCharsets.UTF_8));
            HttpResponse<String> response = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20))
                    .header("Content-Type","application/json").header("Accept","application/json").header("Authorization","Basic "+credentials)
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("statement",statement,"parameters",parameters)))).build(),HttpResponse.BodyHandlers.ofString());
            if (response.statusCode()!=202 && response.statusCode()!=200) throw new IllegalStateException("Neo4j 请求失败，HTTP "+response.statusCode());
            JsonNode root=json.readTree(response.body());
            if (root.path("errors").size()>0 || !root.path("data").path("values").isArray())
                throw new IllegalStateException("Neo4j 查询失败，请检查服务端配置与日志");
            return root.path("data").path("values");
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("图数据库请求已中断"); }
        catch (java.io.IOException e) { throw new IllegalStateException("图数据库连接失败或响应异常"); }
    }
}
