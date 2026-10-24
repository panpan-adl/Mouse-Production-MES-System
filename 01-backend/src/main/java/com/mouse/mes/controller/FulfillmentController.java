package com.mouse.mes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.service.FulfillmentService;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataAccessException;

@RestController
@RequestMapping("/api/fulfillment")
public class FulfillmentController {
    private final FulfillmentService service;private final ObjectMapper json;
    public FulfillmentController(FulfillmentService service,ObjectMapper json){this.service=service;this.json=json;}
    private long actor(HttpServletRequest req){var user=json.convertValue(req.getAttribute("userInfo"),LoginResponse.class);if(!Set.of(5L,9L).contains(user.getRoleId()))throw new SecurityException("仅仓管员或业务管理员可执行库存过账");return user.getId();}
    @GetMapping("/work-orders") public Object works(){return JsonResult.success(service.workOrders());}
    @GetMapping("/work-orders/{id}/materials") public Object materials(@PathVariable long id,@RequestParam(required=false) Long warehouse){return JsonResult.success(service.readiness(id,warehouse));}
    @GetMapping("/work-orders/{id}/trace") public Object trace(@PathVariable long id){return JsonResult.success(service.trace(id));}
    @GetMapping("/stock") public Object stock(){return JsonResult.success(service.stock());}
    @GetMapping("/locations") public Object locations(){return JsonResult.success(service.locations());}
    @GetMapping("/postings") public Object postings(){return JsonResult.success(service.postings());}
    @GetMapping("/purchases") public Object purchases(){return JsonResult.success(service.purchases());}
    @GetMapping("/purchases/{id}/items") public Object items(@PathVariable long id){return JsonResult.success(service.purchaseItems(id));}
    @GetMapping("/inspection-sources") public Object sources(@RequestParam String kind){return JsonResult.success(service.inspectionSources(kind));}
    private Long location(Map<String,Object> v){return v.get("location_id")==null?null:FulfillmentService.n(v.get("location_id"));}
    @PostMapping("/work-orders/{id}/issue") public Object issue(@PathVariable long id,@RequestBody Map<String,Object> v,HttpServletRequest r){long actor=actor(r);return JsonResult.success(service.issue(id,FulfillmentService.n(v.get("warehouse_id")),actor));}
    @PostMapping("/work-orders/{id}/receive") public Object receive(@PathVariable long id,@RequestBody Map<String,Object> v,HttpServletRequest r){long actor=actor(r);return JsonResult.success(service.receiveProduction(id,FulfillmentService.n(v.get("warehouse_id")),location(v),actor));}
    @PostMapping("/purchases/{id}/receive") public Object purchase(@PathVariable long id,@RequestBody Map<String,Object> v,HttpServletRequest r){long actor=actor(r);return JsonResult.success(service.receivePurchase(id,FulfillmentService.n(v.get("warehouse_id")),location(v),actor));}
    @PostMapping("/orders/{id}/ship") public Object ship(@PathVariable long id,@RequestBody Map<String,Object> v,HttpServletRequest r){long actor=actor(r);return JsonResult.success(service.ship(id,FulfillmentService.n(v.get("warehouse_id")),actor));}
    @ExceptionHandler(SecurityException.class) @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN) public Object denied(Exception e){return JsonResult.error(403,e.getMessage());}
    @ExceptionHandler({IllegalArgumentException.class,DataAccessException.class}) @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST) public Object invalid(Exception e){return JsonResult.error(400,e instanceof DataAccessException?"库存或单据关联校验失败，操作未过账":e.getMessage());}
}
