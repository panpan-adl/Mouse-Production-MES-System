package com.mouse.mes.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class IntelligenceService {
    private final JdbcTemplate db;private final ObjectMapper json;private final BusinessQueryService tools;private final TransactionTemplate tx;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Value("${MES_AI_MODE:local}") private String mode;
    @Value("${MES_AI_BASE_URL:}") private String base;
    @Value("${MES_AI_MODEL:}") private String model;
    @Value("${MES_AI_API_KEY:}") private String key;
    public IntelligenceService(JdbcTemplate db,ObjectMapper json,BusinessQueryService tools,PlatformTransactionManager manager){this.db=db;this.json=json;this.tools=tools;tx=new TransactionTemplate(manager);}
    public Map<String,Object> config(){return Map.of("mode",mode,"configured",!key.isBlank()&&!base.isBlank()&&!model.isBlank(),"model",model,"dailyCallLimit",20,"maxOutputTokens",512,"questionLimit",1200,"timeoutSeconds",10);}
    private String id(){return UUID.randomUUID().toString();}
    public Map<String,Object> query(Map<String,Object> body,long actor,int role)throws Exception{
        String question=Objects.toString(body.get("question"),"").trim();if(question.isEmpty()||question.length()>1200)throw new IllegalArgumentException("问题长度应为1至1200字");
        if(Pattern.compile("(?i)(\\b(select|insert|delete|update|drop|alter|union)\\b|密码|密钥|删除数据|修改库存)").matcher(question).find())throw new IllegalArgumentException("仅接受业务数据查询，不执行写入、SQL或敏感信息请求");
        String session=Objects.toString(body.get("sessionId"),"");Map<String,String> filters=new LinkedHashMap<>();int revision=0;
        if(session.isBlank()){session=id();db.update("INSERT INTO mm_ai_session(id,user_id,filters) VALUES(?,?,'{}')",session,actor);}else{var rows=db.queryForList("SELECT filters,revision FROM mm_ai_session WHERE id=? AND user_id=?",session,actor);if(rows.isEmpty())throw new SecurityException("会话不存在或不属于当前用户");filters=json.readValue(rows.get(0).get("filters").toString(),new TypeReference<>(){});revision=((Number)rows.get(0).get("revision")).intValue();}
        String trace=id();long start=System.nanoTime();int input=0,output=0;String status="FAILED";
        try{
            Map<String,Object> parsed;
            if(mode.equals("remote")){
                if(key.isBlank()||base.isBlank()||model.isBlank())throw new IllegalArgumentException("远程模型尚未配置，请在服务器设置接口、模型及密钥");
                reserve(actor);
                String prompt="只返回JSON对象。将中文MES只读问题解析为条件，不计算数量，不输出SQL。允许字段kind(INVENTORY/SHORTAGE/TRACE)、material(物料名称或编码)、warehouse(仓库名称或编码)、product(产品名称)、type(1原料2半成品3成品)、from和to(yyyy-MM-dd，含首尾)、document(完整工单/订单/计划编号)、clarification(条件不明确时的追问)。今天是"+LocalDate.now()+"，已有条件="+json.writeValueAsString(filters)+"。用户只看/改查等是对已有条件的修改。";
                var payload=new LinkedHashMap<String,Object>(Map.of("model",model,"messages",List.of(Map.of("role","system","content",prompt),Map.of("role","user","content",question)),"response_format",Map.of("type","json_object"),"max_tokens",512));
                URI endpoint=URI.create(base.replaceAll("/+$","")+"/chat/completions");if("api.deepseek.com".equals(endpoint.getHost()))payload.put("thinking",Map.of("type","disabled"));if(!Set.of("http","https").contains(endpoint.getScheme()))throw new IllegalArgumentException("模型地址必须为HTTP(S)");
                var response=http.send(HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(10)).header("Authorization","Bearer "+key).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build(),HttpResponse.BodyHandlers.ofString());
                if(response.statusCode()!=200)throw new IllegalArgumentException("模型接口调用失败，HTTP "+response.statusCode()+"；本次未生成查询答案");
                JsonNode root=json.readTree(response.body());input=root.path("usage").path("prompt_tokens").asInt();output=root.path("usage").path("completion_tokens").asInt();
                if(!root.path("choices").path(0).path("finish_reason").asText().equals("stop"))throw new IllegalArgumentException("模型返回不完整，本次未执行查询");
                parsed=json.readValue(root.path("choices").path(0).path("message").path("content").asText(),new TypeReference<>(){});
            }else if(mode.equals("local")){parsed=local(question,filters);}else throw new IllegalArgumentException("模型模式配置无效");
            Set<String> allowed=new HashSet<>(BusinessQueryService.FIELDS);allowed.add("clarification");if(!allowed.containsAll(parsed.keySet()))throw new IllegalArgumentException("解析结果包含非白名单字段，已拒绝执行");
            List<String> clarify=new ArrayList<>();if(parsed.containsKey("clarification")&&!Objects.toString(parsed.get("clarification"),"").isBlank())clarify.add(parsed.get("clarification").toString());
            for(var entry:parsed.entrySet())if(BusinessQueryService.FIELDS.contains(entry.getKey())){if(entry.getValue()==null||entry.getValue().toString().isBlank())filters.remove(entry.getKey());else if(entry.getValue() instanceof Map||entry.getValue() instanceof List)throw new IllegalArgumentException("查询条件格式无效");else filters.put(entry.getKey(),entry.getValue().toString());}
            if(!filters.containsKey("kind"))clarify.add("请选择查询库存、缺料还是工单追溯。");
            resolve(filters,"material","t_material","material_code","material_name",clarify);resolve(filters,"warehouse","t_warehouse","warehouse_code","warehouse_name",clarify);resolve(filters,"product","t_product",null,"product_name",clarify);
            if("TRACE".equals(filters.get("kind"))&&!filters.containsKey("document"))clarify.add("请提供完整工单、订单或计划编号。");
            // Pending unresolved names are saved too, so a follow-up can refine one condition.
            if(db.update("UPDATE mm_ai_session SET filters=?,revision=revision+1,updated_at=NOW() WHERE id=? AND user_id=? AND revision=?",json.writeValueAsString(filters),session,actor,revision)!=1)throw new IllegalArgumentException("同一会话存在并发请求，请重试");
            Map<String,Object> result=new LinkedHashMap<>();result.put("sessionId",session);result.put("traceId",trace);result.put("mode",mode);result.put("conditions",filters);result.put("clarifications",clarify);
            if(clarify.isEmpty()){result.put("result",tools.execute(filters,role));status="SUCCEEDED";}else status="CLARIFICATION";
            result.put("inputTokens",input);result.put("outputTokens",output);return result;
        }catch(java.net.http.HttpTimeoutException ex){throw new IllegalArgumentException("模型接口超时，本次未生成答案，请稍后重试");}
        finally {db.update("INSERT INTO mm_query_audit(id,user_id,session_id,question,mode,status,filters,input_tokens,output_tokens,elapsed_ms) VALUES(?,?,?,?,?,?,?,?,?,?)",trace,actor,session,question,mode,status,json.writeValueAsString(filters),input,output,(System.nanoTime()-start)/1000000);}
    }
    void reserve(long actor){tx.executeWithoutResult(s->{db.update("INSERT IGNORE INTO mm_ai_budget(user_id,day,calls) VALUES(?,CURDATE(),0)",actor);int n=db.update("UPDATE mm_ai_budget SET calls=calls+1 WHERE user_id=? AND day=CURDATE() AND calls<20",actor);if(n==0)throw new IllegalArgumentException("已达到每人每天20次模型调用上限，仍可使用人工条件查询");});}
    private void resolve(Map<String,String> f,String field,String table,String code,String name,List<String> clarify){
        String q=f.get(field);if(q==null)return;List<Map<String,Object>> rows;
        if(q.matches("\\d+"))rows=db.queryForList("SELECT id,"+name+" name FROM "+table+" WHERE id=?",Long.parseLong(q));
        else rows=db.queryForList("SELECT id,"+name+" name FROM "+table+" WHERE "+(code==null?"":"( "+code+"=? OR ")+name+" LIKE ?"+(code==null?"":")")+" LIMIT 12",code==null?new Object[]{"%"+q+"%"}:new Object[]{q,"%"+q+"%"});
        if(rows.size()==1)f.put(field,rows.get(0).get("id").toString());else clarify.add((rows.isEmpty()?"未找到":"存在多个匹配")+" "+field+"："+q+"。请使用精确名称或编号。候选："+rows);
    }
    private Map<String,Object> local(String q,Map<String,String> previous){
        Map<String,Object> r=new LinkedHashMap<>();
        if(q.matches(".*(追溯|工单|订单|计划单).*"))r.put("kind","TRACE");else if(q.matches(".*(缺料|齐套|安全库存|补货).*")){r.put("kind","SHORTAGE");r.put("from",null);r.put("to",null);}else if(q.matches(".*(库存|出入库|入库|出库|原材料|半成品|成品).*"))r.put("kind","INVENTORY");
        if(q.contains("全部仓库"))r.put("warehouse",null);if(q.contains("全部物料"))r.put("material",null);
        if(q.contains("原材料"))r.put("type","1");else if(q.contains("半成品"))r.put("type","2");else if(q.contains("成品"))r.put("type","3");
        LocalDate from=null,to=null,now=LocalDate.now();
        if(q.contains("上个月")){from=now.minusMonths(1).withDayOfMonth(1);to=from.plusMonths(1).minusDays(1);}else if(q.contains("本月")||q.contains("这个月")){from=now.withDayOfMonth(1);to=now;}else if(q.contains("昨天")||q.contains("昨日")){from=now.minusDays(1);to=from;}else if(q.contains("今天")||q.contains("今日")){from=now;to=now;}
        Matcher month=Pattern.compile("(?:(\\d{4})年)?(\\d{1,2})月").matcher(q);if(month.find()){int year=month.group(1)==null?now.getYear():Integer.parseInt(month.group(1));from=LocalDate.of(year,Integer.parseInt(month.group(2)),1);to=from.withDayOfMonth(from.lengthOfMonth());if(to.isAfter(now)&&!from.isAfter(now))to=now;}
        Matcher dates=Pattern.compile("\\d{4}-\\d{2}-\\d{2}").matcher(q);if(dates.find()){from=LocalDate.parse(dates.group());to=dates.find()?LocalDate.parse(dates.group()):from;}
        if(from!=null){r.put("from",from.toString());r.put("to",to.toString());}if(q.contains("当前")||q.contains("清除日期")){r.put("from",null);r.put("to",null);}
        for(var spec:List.of(new String[]{"material","t_material","material_code","material_name"},new String[]{"warehouse","t_warehouse","warehouse_code","warehouse_name"},new String[]{"product","t_product",null,"product_name"})){
            var values=db.queryForList("SELECT id,"+spec[3]+" name"+(spec[2]==null?"":","+spec[2]+" code")+" FROM "+spec[1]);List<Map<String,Object>> matches=new ArrayList<>();for(var v:values){String name=v.get("name").toString();if(q.contains(name)||(v.containsKey("code")&&q.contains(v.get("code").toString())))matches.add(v);}
            if(matches.size()==1)r.put(spec[0],matches.get(0).get("id").toString());else if(matches.size()>1)r.put("clarification","问题包含多个"+spec[0]+"，请一次查询一个明确对象。");
        }
        for(var product:db.queryForList("SELECT id,model,product_code FROM t_product WHERE status=1")){String modelText=Objects.toString(product.get("model"),"");if(!modelText.isBlank()&&q.toUpperCase().contains(modelText.toUpperCase()))r.put("product",product.get("id").toString());}
        if(q.contains("原料仓")&&!r.containsKey("warehouse"))r.put("warehouse","原料仓");
        Matcher doc=Pattern.compile("(?i)(?:MO|WO|SO|PL|PLAN|S2)-[A-Za-z0-9_-]+").matcher(q);if(doc.find())r.put("document",doc.group());
        Matcher entity=Pattern.compile("(?:物料|仓库|产品)[:：]([^，。\\s]+)").matcher(q);while(entity.find()){String full=entity.group();r.put(full.startsWith("物料")?"material":full.startsWith("仓库")?"warehouse":"product",entity.group(1));}
        if(!previous.containsKey("kind")&&!r.containsKey("kind"))r.put("clarification","可查询库存、缺料或追溯。示例：本月原材料库存；物料：精确名称；追溯 MO-完整工单号。");return r;
    }
    public List<Map<String,Object>> history(long actor){return db.queryForList("SELECT id,question,mode,status,input_tokens,output_tokens,elapsed_ms,created_at FROM mm_query_audit WHERE user_id=? ORDER BY created_at DESC LIMIT 30",actor);}
}
