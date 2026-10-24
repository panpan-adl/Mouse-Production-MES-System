package com.mouse.mes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/** Read-only, bounded administration view. Request bodies and credentials are never selected. */
@RestController
@RequestMapping("/api/mes/system-logs")
public class SystemLogController {
    private final JdbcTemplate db;private final ObjectMapper json;
    public SystemLogController(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
    @GetMapping public Object list(@RequestParam(defaultValue="audit") String kind,
        @RequestParam(defaultValue="") String keyword,@RequestParam(defaultValue="") String status,
        @RequestParam(defaultValue="") String startDate,@RequestParam(defaultValue="") String endDate,
        @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request){
        var actor=json.convertValue(request.getAttribute("userInfo"),LoginResponse.class);
        if(actor==null||!Set.of(9L,11L).contains(actor.getRoleId()))throw new SecurityException("只有管理员可以查看系统日志");
        if(page<1||page>100000||size<1||size>100||keyword.length()>100)throw new IllegalArgumentException("分页或搜索条件无效");
        String table,fields,time,search;
        switch(kind){
            case "login" -> {table="t_login_log";time="login_time";
                fields="id,user_id actor_id,username actor_name,user_code,login_time event_time,'登录认证' resource,IF(status=1,'登录成功','登录失败') action,NULL record_id,login_ip ip,CAST(status AS CHAR) status,msg message,browser,os";
                search="CONCAT_WS(' ',username,user_code,login_ip,msg)";}
            case "operation" -> {table="t_operation_log";time="operate_time";
                fields="id,user_id actor_id,username actor_name,user_code,operate_time event_time,module resource,operation action,NULL record_id,operate_ip ip,CAST(status AS CHAR) status,error_msg message,method,SUBSTRING_INDEX(request_url,'?',1) request_url";
                search="CONCAT_WS(' ',username,user_code,module,operation,SUBSTRING_INDEX(request_url,'?',1))";}
            case "audit" -> {table="mm_audit a LEFT JOIN t_user u ON u.id=a.actor_id";time="a.created_at";
                fields="a.id,a.actor_id,COALESCE(u.real_name,'未知或已删除用户') actor_name,u.user_code,a.created_at event_time,a.resource,a.action,a.record_id,NULL ip,'RECORDED' status,NULL message";
                search="CONCAT_WS(' ',u.real_name,u.user_code,a.resource,a.action,a.record_id)";}
            default -> throw new IllegalArgumentException("不支持的日志类型");
        }
        String where=" WHERE 1=1";List<Object> args=new ArrayList<>();
        if(!keyword.isBlank()){where+=" AND "+search+" LIKE ?";args.add("%"+keyword.trim()+"%");}
        if(!status.isBlank()){
            if(kind.equals("audit")||!Set.of("0","1").contains(status))throw new IllegalArgumentException("状态筛选不适用于当前日志类型");
            where+=" AND status=?";args.add(Integer.parseInt(status));
        }
        LocalDate start=startDate.isBlank()?null:LocalDate.parse(startDate),end=endDate.isBlank()?null:LocalDate.parse(endDate);
        if(start!=null&&end!=null&&end.isBefore(start))throw new IllegalArgumentException("结束日期不能早于开始日期");
        if(start!=null){where+=" AND "+time+">=?";args.add(start.atStartOfDay());}
        if(end!=null){where+=" AND "+time+"<?";args.add(end.plusDays(1).atStartOfDay());}
        long total=Objects.requireNonNull(db.queryForObject("SELECT COUNT(*) FROM "+table+where,Long.class,args.toArray()));
        args.add(size);args.add((page-1)*size);
        var items=db.queryForList("SELECT "+fields+" FROM "+table+where+" ORDER BY "+time+" DESC,"+(kind.equals("audit")?"a.id":"id")+" DESC LIMIT ? OFFSET ?",args.toArray());
        return JsonResult.success(Map.of("items",items,"total",total,"page",page,"size",size));
    }
    @ExceptionHandler(SecurityException.class) @ResponseStatus(HttpStatus.FORBIDDEN)
    public Object denied(Exception e){return JsonResult.error(403,e.getMessage());}
    @ExceptionHandler({IllegalArgumentException.class,java.time.DateTimeException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object invalid(Exception e){return JsonResult.error(400,e.getMessage());}
}
