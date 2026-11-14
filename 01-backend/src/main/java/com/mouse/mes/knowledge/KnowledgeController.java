package com.mouse.mes.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import com.mouse.mes.knowledge.rag.RagFileService;
import static com.mouse.mes.knowledge.KnowledgeTypes.*;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeService service;
    private final ObjectMapper json;
    @Autowired(required=false) private RagFileService library;
    public KnowledgeController(KnowledgeService service,ObjectMapper json) { this.service=service; this.json=json; }
    private long owner(HttpServletRequest request) {
        Object raw=request.getAttribute("userInfo");
        if(raw==null) throw new SecurityException("必须登录后访问私有知识库");
        LoginResponse user=json.convertValue(raw,LoginResponse.class);
        if(user.getId()==null) throw new SecurityException("登录身份无效");
        return user.getId();
    }
    @GetMapping("/status") public Object status(@RequestParam(defaultValue="false") boolean probe,HttpServletRequest request) { owner(request); return JsonResult.success(service.status(probe)); }
    @GetMapping("/documents") public Object documents(HttpServletRequest request) { return JsonResult.success(service.documents(owner(request))); }
    @GetMapping("/deletions") public Object pending(HttpServletRequest request) { return JsonResult.success(service.pendingDeletions(owner(request))); }
    @GetMapping("/documents/{id}") public Object document(@PathVariable String id,HttpServletRequest request) { return JsonResult.success(service.document(owner(request),id)); }
    @PostMapping("/documents") public Object upload(@RequestBody DocumentInput input,HttpServletRequest request) {
        long owner=owner(request);
        if(library!=null && input!=null && input.documentId()!=null && library.managed(owner,input.documentId()))throw new IllegalArgumentException("该资料由文件库管理，请通过文件库更新原件");
        return JsonResult.success(service.upsert(owner,input));
    }
    @DeleteMapping("/documents/{id}") public Object delete(@PathVariable String id,@RequestParam long version,HttpServletRequest request) {
        long owner=owner(request);
        if(library!=null && library.managed(owner,id))throw new IllegalArgumentException("该资料由文件库管理，请通过文件库删除以同步两类索引");
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
