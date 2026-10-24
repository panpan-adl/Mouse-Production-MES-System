package com.mouse.mes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.service.FoundationService;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/foundation")
public class FoundationController {
    private final FoundationService service;
    private final ObjectMapper json;
    public FoundationController(FoundationService service,ObjectMapper json){this.service=service;this.json=json;}
    private LoginResponse user(HttpServletRequest request){return json.convertValue(request.getAttribute("userInfo"),LoginResponse.class);}
    private long actor(HttpServletRequest request){return user(request).getId();}
    private void allow(HttpServletRequest request,List<Integer> roles){int role=user(request).getRoleId().intValue();if(role!=9&&!roles.contains(role))throw new SecurityException("当前岗位没有此操作权限");}
    @GetMapping("/catalog") public Object catalog(){return JsonResult.success(service.catalog());}
    @GetMapping("/plans") public Object plans(){return JsonResult.success(service.plans());}
    @GetMapping("/work-orders") public Object workOrders(){return JsonResult.success(service.workOrders());}
    @GetMapping("/work-orders/{id}/tasks") public Object tasks(@PathVariable long id){return JsonResult.success(service.workTasks(id));}
    @PostMapping("/work-orders") public Object create(@RequestBody Map<String,Object> body,HttpServletRequest req)throws Exception{allow(req,List.of(1,2));return JsonResult.success(service.createWorkOrder(body,actor(req)));}
    @GetMapping("/bom/{id}/expand") public Object expand(@PathVariable long id,@RequestParam(defaultValue="1") BigDecimal quantity){return JsonResult.success(service.expand(id,quantity));}
    @GetMapping("/inspections") public Object inspections(){return JsonResult.success(service.inspections());}
    @PostMapping("/inspections") public Object inspect(@RequestBody Map<String,Object> body,HttpServletRequest req)throws Exception{allow(req,List.of(4));return JsonResult.success(service.inspect(body,actor(req)));}
    @GetMapping("/{key}") public Object list(@PathVariable String key){return JsonResult.success(service.list(key));}
    @PostMapping("/{key}") public Object save(@PathVariable String key,@RequestBody Map<String,Object> body,HttpServletRequest req){allow(req,service.definition(key).roles());return JsonResult.success(service.save(key,null,body,actor(req)));}
    @PutMapping("/{key}/{id}") public Object update(@PathVariable String key,@PathVariable long id,@RequestBody Map<String,Object> body,HttpServletRequest req){allow(req,service.definition(key).roles());return JsonResult.success(service.save(key,id,body,actor(req)));}
    @DeleteMapping("/{key}/{id}") public Object delete(@PathVariable String key,@PathVariable long id,HttpServletRequest req){allow(req,service.definition(key).roles());service.delete(key,id,actor(req));return JsonResult.success();}
    @ExceptionHandler(SecurityException.class) @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
    public Object denied(SecurityException error){return JsonResult.error(403,error.getMessage());}
    @ExceptionHandler({IllegalArgumentException.class,NoSuchElementException.class,DataAccessException.class})
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public Object invalid(Exception error){return JsonResult.error(400,error instanceof DataAccessException?"数据关联或唯一性校验失败，请检查引用和编码":error.getMessage());}
}
