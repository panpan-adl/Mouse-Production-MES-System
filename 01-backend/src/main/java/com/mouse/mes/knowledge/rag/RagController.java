package com.mouse.mes.knowledge.rag;

import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.mouse.mes.knowledge.KnowledgeService;
import com.mouse.mes.knowledge.KnowledgeTypes;

@RestController
@RequestMapping("/api/rag")
public class RagController {
    private final RagFileService files;
    private final RagVectorService vectors;
    private final RagModelClient models;
    @Autowired(required=false) private KnowledgeService knowledge;
    public RagController(RagFileService files,RagVectorService vectors,RagModelClient models) {this.files=files;this.vectors=vectors;this.models=models;}
    private long owner(HttpServletRequest request) {
        if(!(request.getAttribute("userInfo") instanceof LoginResponse identity)||identity.getId()==null||identity.getId()<1)
            throw new SecurityException("请先登录");
        if(request.getParameter("owner")!=null||request.getParameter("userId")!=null) throw new SecurityException("不接受客户端指定用户身份");
        return identity.getId();
    }
    @GetMapping("/status") public Object status(HttpServletRequest request) {owner(request);var status=new LinkedHashMap<String,Object>(models.status());status.put("graphAvailable",files.graphIntegrated());return JsonResult.success(status);}
    @GetMapping("/documents") public Object list(HttpServletRequest request) {return JsonResult.success(files.list(owner(request)));}
    @GetMapping("/events") public Object events(HttpServletRequest request) {return JsonResult.success(files.events(owner(request)));}
    @GetMapping("/documents/{id}") public Object detail(HttpServletRequest request,@PathVariable String id) {return JsonResult.success(files.detail(owner(request),id));}
    @PostMapping(value="/documents",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object upload(HttpServletRequest request,@RequestParam String documentId,@RequestParam int version,
                         @RequestParam String title,@RequestParam MultipartFile file) throws Exception {
        long user=owner(request);
        if(file.getSize()>RagParser.MAX_BYTES) throw new IllegalArgumentException("文件最大2MB");
        return JsonResult.success(files.upload(user,documentId,version,title,file.getOriginalFilename(),file.getBytes()));
    }
    @PostMapping("/documents/{id}/retry") public Object retry(HttpServletRequest request,@PathVariable String id,@RequestParam int version) throws Exception {
        return JsonResult.success(files.retry(owner(request),id,version));
    }
    @DeleteMapping("/documents/{id}") public Object delete(HttpServletRequest request,@PathVariable String id,@RequestParam int version) {
        return JsonResult.success(files.delete(owner(request),id,version));
    }
    @GetMapping("/documents/{id}/download") public ResponseEntity<byte[]> download(HttpServletRequest request,@PathVariable String id) throws Exception {
        var result=files.download(owner(request),id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(result.filename(),StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).body(result.bytes());
    }
    @GetMapping("/documents/{id}/sync-payload") public Object payload(HttpServletRequest request,@PathVariable String id) {
        return JsonResult.success(files.syncPayload(owner(request),id));
    }
    @GetMapping("/events/{id}/{version}/sync-payload") public Object eventPayload(HttpServletRequest request,@PathVariable String id,@PathVariable int version) {
        return JsonResult.success(files.syncEvent(owner(request),id,version));
    }
    @PostMapping("/messages") public Object ask(HttpServletRequest request,@RequestBody Map<String,Object> input) throws Exception {
        long user=owner(request);
        if(!Set.of("question","mode","documentIds").containsAll(input.keySet())) throw new IllegalArgumentException("只接受question、mode和documentIds，身份由登录会话确定");
        if(knowledge!=null&&files.graphIntegrated()) {
            List<String> ids=new ArrayList<>();
            if(input.containsKey("documentIds")) {
                if(!(input.get("documentIds") instanceof List<?> values))throw new IllegalArgumentException("documentIds必须为数组");
                for(Object value:values){if(!(value instanceof String text))throw new IllegalArgumentException("资料编号必须为字符串");ids.add(text);}
            }
            return JsonResult.success(knowledge.ask(user,new KnowledgeTypes.Question(Objects.toString(input.get("question"),""),Objects.toString(input.get("mode"),"RAG"),ids)));
        }
        if(input.containsKey("documentIds"))throw new IllegalStateException("统一图谱模块未启用，暂不支持指定资料范围");
        return JsonResult.success(vectors.ask(user,Objects.toString(input.get("question"),""),Objects.toString(input.get("mode"),"RAG")));
    }
    @ExceptionHandler(SecurityException.class) public ResponseEntity<?> denied(SecurityException e) {return ResponseEntity.status(403).body(JsonResult.error(403,e.getMessage()));}
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<?> invalid(IllegalArgumentException e) {return ResponseEntity.badRequest().body(JsonResult.error(400,e.getMessage()));}
    @ExceptionHandler(IllegalStateException.class) public ResponseEntity<?> unavailable(IllegalStateException e) {return ResponseEntity.status(503).body(JsonResult.error(503,e.getMessage()));}
    @ExceptionHandler(Exception.class) public ResponseEntity<?> failure(Exception e) {return ResponseEntity.status(503).body(JsonResult.error(503,"知识库处理失败，请检查服务日志或联系管理员"));}
}
