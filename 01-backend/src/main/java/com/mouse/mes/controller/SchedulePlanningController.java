package com.mouse.mes.controller;
import com.mouse.mes.service.FoundationService;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.util.JsonResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.*;
@RestController
@RequestMapping("/api/foundation/schedule-planning")
public class SchedulePlanningController {
 private final FoundationService service;private final JdbcTemplate db;private final ObjectMapper json;
 public SchedulePlanningController(FoundationService service,JdbcTemplate db,ObjectMapper json){this.service=service;this.db=db;this.json=json;}
 @PostMapping("/copy-week") @Transactional public Object copy(@RequestBody Map<String,Object> body,HttpServletRequest req){
  var user=json.convertValue(req.getAttribute("userInfo"),LoginResponse.class);
  if(!Set.of(1L,2L,9L).contains(user.getRoleId()))throw new SecurityException("只有生产管理岗位可复制排班");
  LocalDate from=LocalDate.parse(body.get("week_start").toString());LocalDateTime start=from.atStartOfDay();
  db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
  var rows=db.queryForList("SELECT * FROM mm_schedule WHERE start_at>=? AND start_at<?",start,start.plusDays(7));
  if(rows.isEmpty())throw new IllegalArgumentException("该周没有排班可复制");
  for(var row:rows){var v=new LinkedHashMap<String,Object>(row);v.put("start_at",com.mouse.mes.service.PeriodicMaintenanceService.dateTime(row.get("start_at")).plusWeeks(1).toString());v.put("end_at",com.mouse.mes.service.PeriodicMaintenanceService.dateTime(row.get("end_at")).plusWeeks(1).toString());service.save("schedules",null,v,user.getId());}
  return JsonResult.success(rows.size());
 }
 @ExceptionHandler(SecurityException.class) @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN) public Object denied(Exception e){return JsonResult.error(e.getMessage());}
 @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST) public Object invalid(Exception e){return JsonResult.error(e.getMessage());}
}
