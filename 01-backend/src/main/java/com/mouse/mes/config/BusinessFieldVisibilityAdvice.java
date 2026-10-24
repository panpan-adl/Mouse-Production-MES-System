package com.mouse.mes.config;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mouse.mes.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** The same price boundary applies to legacy product endpoints and the new catalog. */
@RestControllerAdvice
public class BusinessFieldVisibilityAdvice implements ResponseBodyAdvice<Object> {
    private final ObjectMapper json;private final HttpServletRequest request;
    public BusinessFieldVisibilityAdvice(ObjectMapper json,HttpServletRequest request){this.json=json;this.request=request;}
    @Override public boolean supports(MethodParameter p,Class<? extends HttpMessageConverter<?>> converter){return !org.springframework.http.converter.StringHttpMessageConverter.class.isAssignableFrom(converter)&&!org.springframework.http.converter.ByteArrayHttpMessageConverter.class.isAssignableFrom(converter);}
    @Override public Object beforeBodyWrite(Object body,MethodParameter p,MediaType media,Class<? extends HttpMessageConverter<?>> converter,ServerHttpRequest req,ServerHttpResponse response){
        Object identity=request.getAttribute("userInfo");if(identity==null||body==null)return body;LoginResponse user=json.convertValue(identity,LoginResponse.class);
        if(Set.of(1L,7L,9L,10L).contains(user.getRoleId()))return body;
        JsonNode tree=json.valueToTree(body);clean(tree);return tree;
    }
    private void clean(JsonNode node){if(node.isObject()){ObjectNode object=(ObjectNode)node;object.remove(List.of("cost_price","selling_price","costPrice","sellingPrice"));object.elements().forEachRemaining(this::clean);}else if(node.isArray())node.elements().forEachRemaining(this::clean);}
}
