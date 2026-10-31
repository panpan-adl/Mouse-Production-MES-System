package com.mouse.mes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.service.*;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class IntelligenceController {
    private final BusinessQueryService tools;private final IntelligenceService ai;private final AutomationService automation;private final ObjectMapper json;
    public IntelligenceController(BusinessQueryService tools,IntelligenceService ai,AutomationService automation,ObjectMapper json){this.tools=tools;this.ai=ai;this.automation=automation;this.json=json;}
    private LoginResponse user(HttpServletRequest r){return json.convertValue(r.getAttribute("userInfo"),LoginResponse.class);}
    private void allow(HttpServletRequest r,Integer...roles){if(!Set.of(roles).contains(user(r).getRoleId().intValue()))throw new SecurityException("当前岗位没有此操作权限");}
    @GetMapping("/ai/config") public Object config(){return JsonResult.success(ai.config());}
    @GetMapping("/ai/history") public Object history(HttpServletRequest r){return JsonResult.success(ai.history(user(r).getId()));}
    @GetMapping("/analytics/catalog") public Object catalog(HttpServletRequest r){return JsonResult.success(tools.catalog(user(r).getRoleId().intValue()));}
    @PostMapping("/analytics/query") public Object manual(@RequestBody Map<String,String> f,HttpServletRequest r){return JsonResult.success(tools.execute(f,user(r).getRoleId().intValue()));}
    @PostMapping("/ai/query") public Object query(@RequestBody Map<String,Object> body,HttpServletRequest r)throws Exception{return JsonResult.success(ai.query(body,user(r).getId(),user(r).getRoleId().intValue()));}
    @GetMapping("/automation") public Object automation(HttpServletRequest r){allow(r,1,2,4,6,9);return JsonResult.success(automation.state());}
    @PostMapping("/automation/rule") public Object rule(@RequestBody Map<String,Boolean> body,HttpServletRequest r){allow(r,1,2,9);automation.toggle(Boolean.TRUE.equals(body.get("enabled")));return JsonResult.success();}
    @PostMapping("/automation/retry/{id}") public Object retry(@PathVariable long id,HttpServletRequest r){allow(r,1,2,4,9);automation.retry(id);return JsonResult.success();}
    @PostMapping("/automation/process") public Object process(HttpServletRequest r){allow(r,1,2,4,9);for(int i=0;i<20&&automation.process();i++){}return JsonResult.success(automation.state());}
    @GetMapping("/automation/comparisons") public Object comparisons(HttpServletRequest r){allow(r,1,2,4,9);return JsonResult.success(automation.comparison());}
    @PostMapping("/automation/comparisons/{task}/start") public Object beginComparison(@PathVariable long task,HttpServletRequest r){allow(r,4,9);automation.beginComparison(task,user(r).getId());return JsonResult.success();}
    @PostMapping("/automation/comparisons/{task}/complete") public Object finishComparison(@PathVariable long task,HttpServletRequest r){allow(r,4,9);automation.finishComparison(task,user(r).getId());return JsonResult.success();}
    @GetMapping("/automation/suggestions") public Object suggestions(HttpServletRequest r){allow(r,1,2,5,6,9);return JsonResult.success(automation.suggestions());}
    @PostMapping("/automation/suggestions/{id}/confirm") public Object confirm(@PathVariable long id,HttpServletRequest r){allow(r,6,9);automation.confirm(id,user(r).getId());return JsonResult.success();}
    @ExceptionHandler(SecurityException.class) @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN) public Object denied(Exception e){return JsonResult.error(403,e.getMessage());}
    @ExceptionHandler({IllegalArgumentException.class,java.io.IOException.class,InterruptedException.class}) @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST) public Object invalid(Exception e){return JsonResult.error(400,e instanceof IllegalArgumentException?e.getMessage():"模型接口或返回格式异常，本次没有查询答案");}
}
