package com.mouse.mes.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.util.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Records successful workflow actions without storing request secrets. */
@RestControllerAdvice(assignableTypes=com.mouse.mes.controller.MesCompletionController.class)
public class MesAuditAspect implements ResponseBodyAdvice<Object> {
    private final JdbcTemplate db;private final HttpServletRequest request;private final ObjectMapper json;
    public MesAuditAspect(JdbcTemplate db,HttpServletRequest request,ObjectMapper json){this.db=db;this.request=request;this.json=json;}
    public boolean supports(MethodParameter p,Class<? extends HttpMessageConverter<?>> converter){return true;}
    public Object beforeBodyWrite(Object result,MethodParameter p,MediaType media,Class<? extends HttpMessageConverter<?>> converter,ServerHttpRequest req,ServerHttpResponse response){
        if(!"POST".equals(request.getMethod())||request.getAttribute("userInfo")==null)return result;
        com.fasterxml.jackson.databind.JsonNode tree=json.valueToTree(result);
        if(tree.path("code").asInt()!=200)return result;
        long actor=json.convertValue(request.getAttribute("userInfo"),LoginResponse.class).getId();
        db.update("INSERT INTO mm_audit(actor_id,resource,action,record_id) VALUES(?,?,?,0)",actor,"mes:"+p.getMethod().getName(),request.getMethod());
        return result;
    }
}
