package com.mouse.mes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.service.SkillAgentService;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class SkillAgentController {
    private final SkillAgentService agent;private final ObjectMapper json;
    public SkillAgentController(SkillAgentService agent,ObjectMapper json){this.agent=agent;this.json=json;}
    private LoginResponse user(HttpServletRequest r){return json.convertValue(r.getAttribute("userInfo"),LoginResponse.class);}
    @GetMapping("/skills") public Object skills(HttpServletRequest r){return JsonResult.success(agent.catalog(user(r).getRoleId().intValue()));}
    @GetMapping("/runs") public Object history(HttpServletRequest r){return JsonResult.success(agent.history(user(r).getId(),user(r).getRoleId().intValue()));}
    @GetMapping("/runs/{id}") public Object detail(@PathVariable String id,HttpServletRequest r){return JsonResult.success(agent.detailForRole(id,user(r).getId(),user(r).getRoleId().intValue()));}
    @PostMapping("/messages") public Object message(@RequestBody Map<String,Object> body,HttpServletRequest r) throws Exception{return JsonResult.success(agent.message(body,user(r).getId(),user(r).getRoleId().intValue()));}
    @PostMapping("/proposals/{material}") public Object propose(@PathVariable long material,HttpServletRequest r){return JsonResult.success(agent.propose(material,user(r).getId(),user(r).getRoleId().intValue()));}
    @PostMapping("/runs/{id}/confirm") public Object confirm(@PathVariable String id,HttpServletRequest r){return JsonResult.success(agent.confirm(id,user(r).getId(),user(r).getRoleId().intValue()));}
    @PostMapping("/runs/{id}/cancel") public Object cancel(@PathVariable String id,HttpServletRequest r){return JsonResult.success(agent.cancel(id,user(r).getId()));}
    @ExceptionHandler(SecurityException.class) @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN) public Object denied(Exception e){return JsonResult.error(403,e.getMessage());}
    @ExceptionHandler({IllegalArgumentException.class,java.io.IOException.class,InterruptedException.class}) @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST) public Object invalid(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();return JsonResult.error(400,e instanceof IllegalArgumentException?e.getMessage():"大模型接口超时或格式异常，本次未返回业务结论，请稍后重试");}
}
