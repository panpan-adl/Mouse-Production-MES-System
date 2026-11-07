package com.mouse.mes.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import static com.mouse.mes.knowledge.KnowledgeTypes.*;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private com.mouse.mes.knowledge.rag.RagFileService files;
    private final KnowledgeService service;
    private final ObjectMapper json;
    public KnowledgeController(KnowledgeService service,ObjectMapper json) { this.service=service; this.json=json; }
    private long owner(HttpServletRequest request) {
        Object raw=request.getAttribute("userInfo");
        if(raw==null) throw new SecurityException("必须登录后访问私有知识库");
        LoginResponse user=json.convertValue(raw,LoginResponse.class);
        if(user.getId()==null) throw new SecurityException("登录身份无效");
        return user.getId();
    }
    @GetMapping("/status") public Object status(@RequestParam(defaultValue="false") boolean probe,HttpServletRequest request) { owner(request); return JsonResult.success(service.status(probe)); }
    @GetMapping("/documents") public Object documents(HttpServletRequest request) {
        long user=owner(request);
        if(files==null)return JsonResult.success(service.documents(user));
        var documents=new java.util.ArrayList<Map<String,Object>>(files.list(user));
        var known=new java.util.HashSet<String>();documents.forEach(d->known.add(d.get("documentId").toString()));
        for(var doc:service.documents(user)) if(!known.contains(doc.get("documentId"))) documents.add(doc);
        return JsonResult.success(documents);
    }
    @GetMapping("/deletions") public Object pending(HttpServletRequest request) {
        long user=owner(request);var rows=new java.util.LinkedHashMap<String,Map<String,Object>>();
        service.pendingDeletions(user).forEach(d->rows.put(d.get("documentId").toString(),d));
        if(files!=null)files.pendingCleanup(user).forEach(d->rows.put(d.get("documentId").toString(),d));
        return JsonResult.success(List.copyOf(rows.values()));
    }
    @GetMapping("/documents/{id}") public Object document(@PathVariable String id,HttpServletRequest request) { return JsonResult.success(service.document(owner(request),id)); }
    @PostMapping("/documents") public Object upload(@RequestBody DocumentInput input,HttpServletRequest request) throws Exception {
        long user=owner(request);KnowledgeService.validate(input);
        if(files==null)return JsonResult.success(service.upsert(user,input));
        if(files.isManaged(user,input.documentId())) {
            var existing=files.detail(user,input.documentId());
            if(((Number)existing.get("version")).longValue()==input.version()) {
                if(!input.title().equals(existing.get("title"))||!input.text().equals(existing.get("text"))) throw new IllegalArgumentException("同版本内容冲突");
                return JsonResult.success(files.retry(user,input.documentId(),Math.toIntExact(input.version())));
            }
        }
        return JsonResult.success(files.upload(user,input.documentId(),Math.toIntExact(input.version()),input.title(),input.title()+".md",input.text().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    @DeleteMapping("/documents/{id}") public Object delete(@PathVariable String id,@RequestParam long version,HttpServletRequest request) {
        long owner=owner(request);
        if(files!=null&&files.isManaged(owner,id)) return JsonResult.success(files.delete(owner,id,Math.toIntExact(version)));
        service.tombstone(owner,id,version); return JsonResult.success(service.cleanup(owner,id,version));
    }
    @GetMapping("/graph") public Object graph(@RequestParam(required=false) List<String> documentIds,
            @RequestParam(defaultValue="") String entity,HttpServletRequest request) {
        return JsonResult.success(service.visualize(owner(request),documentIds,entity));
    }
    @PostMapping("/model-probe") public Object probe(HttpServletRequest request) { return JsonResult.success(service.probeModel(owner(request))); }
    @PostMapping("/messages") public Object ask(@RequestBody Question input,HttpServletRequest request) { return JsonResult.success(service.ask(owner(request),input)); }
    @ExceptionHandler(SecurityException.class) @ResponseStatus(HttpStatus.FORBIDDEN)
    public Object denied(SecurityException e) { return JsonResult.error(403,e.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object invalid(IllegalArgumentException e) { return JsonResult.error(400,e.getMessage()); }
    @ExceptionHandler(IllegalStateException.class) @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Object unavailable(IllegalStateException e) { return JsonResult.error(503,e.getMessage()); }
}
