package com.mouse.mes.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Decompose unissued assembly orders into explicitly configured semi-finished product orders. */
@Service
public class MesWorkBreakdownService {
    private final JdbcTemplate db;private final ObjectMapper json;private final MesOperationsService operations;private final LineStockService line;
    public MesWorkBreakdownService(JdbcTemplate db,ObjectMapper json,MesOperationsService operations,LineStockService line){this.db=db;this.json=json;this.operations=operations;this.line=line;}
    static long id(Object v){return MesRulesService.id(v);}static BigDecimal dec(Object v){return MesRulesService.dec(v);}static void require(boolean b,String m){MesRulesService.require(b,m);}
    private long count(String s,Object...args){return Objects.requireNonNull(db.queryForObject(s,Long.class,args));}
    @Transactional(rollbackFor=Exception.class)
    public List<Long> decompose(long wo,long owner,long actor)throws Exception{db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");List<Long> created=new ArrayList<>();walk(wo,owner,actor,new HashSet<>(),created);return created;}
    private void walk(long wo,long owner,long actor,Set<Long> path,List<Long> created)throws Exception{
        require(path.size()<20&&created.size()<100,"工单分解层级或数量过大");
        var w=db.queryForMap("SELECT * FROM mm_work_order WHERE id=? FOR UPDATE",wo);var product=db.queryForMap("SELECT * FROM mm_work_product WHERE work_order_id=?",wo);long root=id(product.get("material_id"));require(path.add(root),"跨产品BOM存在循环，不能生成子工单");
        require(count("SELECT COUNT(*) FROM mm_work_relation WHERE parent_work_order_id=?",wo)==0,"该工单已经分解，不能重复生成");
        require(count("SELECT COUNT(*) FROM mm_stock_posting WHERE source_id=? AND kind='ISSUE'",wo)==0&&count("SELECT COUNT(*) FROM mm_document WHERE source_id=? AND kind='LINE_ISSUE' AND status='POSTED'",wo)==0,"已领料工单不能重新分解");
        require(count("SELECT COUNT(*) FROM t_production_report r JOIN mm_task_context c ON c.task_id=r.task_id WHERE c.work_order_id=?",wo)==0,"已报工工单不能分解");
        List<Map<String,Object>> lines=json.readValue(w.get("bom_snapshot").toString(),new TypeReference<>(){});Map<Long,BigDecimal> cut=new LinkedHashMap<>();cut(root,root,BigDecimal.ONE,lines,cut,new HashSet<>());
        var semis=cut.keySet().stream().filter(m->m!=root&&count("SELECT COUNT(*) FROM t_material WHERE id=? AND material_type=2 AND sourcing_mode<>'BUY'",m)==1).toList();if(semis.isEmpty()){path.remove(root);return;}
        // No posting exists: replace the planned assembly requirements before execution, preserving the original BOM snapshot.
        db.update("DELETE FROM mm_work_material WHERE work_order_id=?",wo);db.update("DELETE FROM mm_work_consumption WHERE work_order_id=?",wo);db.update("DELETE FROM mm_work_output WHERE work_order_id=?",wo);
        for(var leaf:cut.entrySet()){var material=db.queryForMap("SELECT * FROM t_material WHERE id=?",leaf.getKey());db.update("INSERT INTO mm_work_material(work_order_id,material_id,material_code,material_name,unit,required_quantity) VALUES(?,?,?,?,?,?)",wo,leaf.getKey(),material.get("material_code"),material.get("material_name"),material.get("unit"),leaf.getValue().multiply(dec(w.get("quantity"))));}
        List<Map<String,Object>> route=json.readValue(w.get("route_snapshot").toString(),new TypeReference<>(){});line.snapshot(wo,route,root,dec(w.get("quantity")));
        for(long semi:semis){
            var options=db.queryForList("SELECT b.id bom_id,b.product_id,r.id route_id FROM mm_bom b JOIN mm_product_material m ON m.product_id=b.product_id AND m.material_id=b.root_material_id JOIN mm_route r ON r.product_id=b.product_id WHERE b.root_material_id=? ORDER BY b.id DESC,r.id DESC LIMIT 1",semi);require(!options.isEmpty(),"半成品缺少产品映射、BOM版本或路线，请先维护："+semi);var option=options.get(0);
            Map<String,Object> request=new LinkedHashMap<>(option);request.put("quantity",cut.get(semi).multiply(dec(w.get("quantity"))).intValueExact());request.put("owner_id",owner);var child=operations.createWork(request,actor);long childId=id(child.get("id"));db.update("INSERT INTO mm_work_relation(parent_work_order_id,child_work_order_id,material_id) VALUES(?,?,?)",wo,childId,semi);created.add(childId);walk(childId,owner,actor,new HashSet<>(path),created);
        }
        path.remove(root);
    }
    private void cut(long root,long node,BigDecimal quantity,List<Map<String,Object>> lines,Map<Long,BigDecimal> cut,Set<Long> active){
        require(active.add(node),"BOM存在循环");var children=lines.stream().filter(r->id(r.get("parent_material_id"))==node).toList();
        boolean semi=node!=root&&count("SELECT COUNT(*) FROM t_material WHERE id=? AND material_type=2",node)==1;
        if(semi||children.isEmpty())cut.merge(node,quantity,BigDecimal::add);else for(var child:children)cut(root,id(child.get("material_id")),quantity.multiply(dec(child.get("quantity"))),lines,cut,active);active.remove(node);
    }
    public List<Map<String,Object>> relations(){return db.queryForList("SELECT r.*,p.code parent_code,c.code child_code,m.material_name FROM mm_work_relation r JOIN mm_work_order p ON p.id=r.parent_work_order_id JOIN mm_work_order c ON c.id=r.child_work_order_id JOIN t_material m ON m.id=r.material_id ORDER BY r.id");}
}
