package com.mouse.mes.controller;
import com.mouse.mes.service.BusinessNotificationService;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.util.JsonResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/mes/business-notices")
public class BusinessNotificationController {
 private final BusinessNotificationService service;private final ObjectMapper mapper;
 public BusinessNotificationController(BusinessNotificationService service,ObjectMapper mapper){this.service=service;this.mapper=mapper;}
 private LoginResponse user(HttpServletRequest r){return mapper.convertValue(r.getAttribute("userInfo"),LoginResponse.class);}
 @GetMapping public Object list(HttpServletRequest r){var u=user(r);return JsonResult.success(service.list(u.getId(),u.getRoleId()));}
 @PostMapping("/{id}/read") public Object read(@PathVariable long id,HttpServletRequest r){service.read(id,user(r).getId());return JsonResult.success();}
 @ExceptionHandler(SecurityException.class) @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN) public Object denied(SecurityException e){return JsonResult.error(403,e.getMessage());}
}
