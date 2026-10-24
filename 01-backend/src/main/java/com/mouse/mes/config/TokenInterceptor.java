package com.mouse.mes.config;

import com.mouse.mes.service.RedisService;
import com.mouse.mes.util.JsonResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired private com.mouse.mes.mapper.UserMapper userMapper;

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/logout"
    );

    private static final List<String> WHITE_LIST_PREFIX = Arrays.asList(
            "/api/users/avatar/"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUri = request.getRequestURI();

        if (WHITE_LIST.contains(requestUri)) {
            return true;
        }

        for (String prefix : WHITE_LIST_PREFIX) {
            if (requestUri.startsWith(prefix)) {
                return true;
            }
        }

        // Test bypass is explicit and isolated; classpath contents never disable authentication.
        if ("test".equals(System.getProperty("spring.profiles.active")) && "true".equals(System.getProperty("test.mode"))) return true;

        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            PrintWriter writer = response.getWriter();
            writer.write(objectMapper.writeValueAsString(JsonResult.error(401, "未登录，请先登录")));
            writer.flush();
            writer.close();
            return false;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Object userInfo = redisService.get("user:token:" + token);
        if (userInfo == null) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            PrintWriter writer = response.getWriter();
            writer.write(objectMapper.writeValueAsString(JsonResult.error(401, "登录已过期，请重新登录")));
            writer.flush();
            writer.close();
            return false;
        }

        com.mouse.mes.dto.LoginResponse identity = objectMapper.convertValue(userInfo, com.mouse.mes.dto.LoginResponse.class);
        var current=userMapper.findById(identity.getId());
        if(current==null||current.getStatus()==null||current.getStatus()==0||Integer.valueOf(1).equals(current.getDeleted())||!java.util.Objects.equals(current.getRoleId(),identity.getRoleId())){
            redisService.delete("user:token:"+token);response.setStatus(401);response.setContentType("application/json;charset=UTF-8");response.getWriter().write(objectMapper.writeValueAsString(JsonResult.error(401,"账号状态或岗位已变更，请重新登录")));return false;
        }
        if (("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))
                && !RolePermissions.canRead(identity.getRoleId().intValue(), requestUri)
                && !requestUri.equals("/api/users/" + identity.getId())) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(JsonResult.error(403, "当前岗位没有读取此数据的权限")));
            return false;
        }
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod()) && !"OPTIONS".equals(request.getMethod())
                && !RolePermissions.canWrite(identity.getRoleId().intValue(), requestUri, request.getMethod())) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(JsonResult.error(403, "当前岗位没有此操作权限")));
            return false;
        }
        request.setAttribute("userInfo", userInfo);
        return true;
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        writer.write(objectMapper.writeValueAsString(JsonResult.error(message)));
        writer.flush();
        writer.close();
    }
}
