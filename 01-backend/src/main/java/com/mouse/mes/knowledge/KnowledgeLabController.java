package com.mouse.mes.knowledge;

import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/knowledge/lab")
public class KnowledgeLabController {
    private final KnowledgeLabService service;
    public KnowledgeLabController(KnowledgeLabService service){this.service=service;}
    private long owner(HttpServletRequest request){
        if(!(request.getAttribute("userInfo") instanceof LoginResponse user)||user.getId()==null||user.getId()<1)throw new SecurityException("请先登录");
        if(request.getParameter("owner")!=null||request.getParameter("userId")!=null)throw new SecurityException("用户身份仅由登录会话确定");return user.getId();
    }
    @GetMapping("/versions/{id}") public Object versions(HttpServletRequest r,@PathVariable String id){return JsonResult.success(service.versions(owner(r),id));}
    @GetMapping("/diff/{id}") public Object diff(HttpServletRequest r,@PathVariable String id,@RequestParam long before,@RequestParam long after){return JsonResult.success(service.diff(owner(r),id,before,after));}
    @PostMapping("/compare") public Object compare(HttpServletRequest r,@RequestBody KnowledgeTypes.Question input){return JsonResult.success(service.compare(owner(r),input));}
    @PostMapping("/dialogue") public Object dialogue(HttpServletRequest r,@RequestBody KnowledgeLabService.DialogueInput input){return JsonResult.success(service.dialogue(owner(r),input));}
    @GetMapping("/quality") public Object quality(HttpServletRequest r,@RequestParam(required=false) List<String> documentIds){return JsonResult.success(service.quality(owner(r),documentIds));}
    @PostMapping("/review") public Object review(HttpServletRequest r,@RequestBody KnowledgeLabService.Review input){service.review(owner(r),input);return JsonResult.success(Map.of("saved",true));}
    @GetMapping("/association") public Object association(HttpServletRequest r,@RequestParam String entity,@RequestParam(required=false) List<String> documentIds){return JsonResult.success(service.association(owner(r),entity,documentIds));}
    @ExceptionHandler(SecurityException.class) public ResponseEntity<?> denied(SecurityException e){return ResponseEntity.status(403).body(JsonResult.error(403,e.getMessage()));}
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<?> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(JsonResult.error(400,e.getMessage()));}
    @ExceptionHandler(IllegalStateException.class) public ResponseEntity<?> failure(IllegalStateException e){return ResponseEntity.status(503).body(JsonResult.error(503,e.getMessage()));}
}
