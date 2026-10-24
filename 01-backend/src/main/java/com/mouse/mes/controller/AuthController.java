package com.mouse.mes.controller;

import com.mouse.mes.dto.LoginRequest;
import com.mouse.mes.dto.LoginResponse;
import com.mouse.mes.entity.Department;
import com.mouse.mes.entity.LoginLog;
import com.mouse.mes.entity.OperationLog;
import com.mouse.mes.entity.Role;
import com.mouse.mes.entity.User;
import com.mouse.mes.mapper.DepartmentMapper;
import com.mouse.mes.mapper.LoginLogMapper;
import com.mouse.mes.mapper.OperationLogMapper;
import com.mouse.mes.mapper.RoleMapper;
import com.mouse.mes.mapper.UserMapper;
import com.mouse.mes.service.RedisService;
import com.mouse.mes.service.UserService;
import com.mouse.mes.util.JsonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired private com.mouse.mes.service.RoleAccessService roleAccess;
    @Autowired
    private RedisService redisService;

    @PostMapping("/login")
    public JsonResult<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String userCode = request.getUserCode();
        String username = request.getUsername();
        logger.info("用户登录请求 - 用户编码: {}, 用户名: {}", userCode, username);
        
        User user = null;
        if (username != null && !username.isEmpty()) {
            user = userService.findByUsername(username);
        } else if (userCode != null && !userCode.isEmpty()) {
            user = userService.findByUserCode(userCode);
        }
        
        if (user == null) {
            logger.warn("登录失败 - 用户不存在: {}", userCode);
            saveLoginLog(null, userCode, httpRequest, 0, "用户不存在", userCode);
            return JsonResult.error("用户编码或密码错误");
        }
        
        if (!request.getPassword().equals(user.getPassword())) {
            logger.warn("登录失败 - 密码错误: {}", userCode);
            saveLoginLog(user.getId(), user.getUserCode(), httpRequest, 0, "密码错误", user.getUserCode());
            return JsonResult.error("用户编码或密码错误");
        }
        
        if (user.getStatus() == 0 || Integer.valueOf(1).equals(user.getDeleted())) {
            logger.warn("登录失败 - 用户已禁用: {}", userCode);
            saveLoginLog(user.getId(), user.getUserCode(), httpRequest, 0, "用户已禁用", user.getUserCode());
            return JsonResult.error("用户已禁用");
        }
        
        Role role = roleMapper.findById(user.getRoleId() != null ? user.getRoleId().intValue() : null);
        if(role==null||!Integer.valueOf(1).equals(role.getStatus()))return JsonResult.error("岗位已停用，请联系管理员");
        long permissionRole;try{permissionRole=roleAccess.effective(user.getRoleId());}catch(SecurityException e){return JsonResult.error(e.getMessage());}
        String roleName = role.getRoleName();
        
        Department dept = null;
        String deptName = "";
        if (user.getDeptId() != null) {
            dept = departmentMapper.findById(user.getDeptId());
            deptName = dept != null ? dept.getDeptName() : "";
        }
        
        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setUserCode(user.getUserCode());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setGender(user.getGender());
        response.setAvatar(user.getAvatar());
        response.setRoleId(user.getRoleId());
        response.setPermissionRoleId(permissionRole);
        response.setRoleName(roleName);
        response.setDeptId(user.getDeptId());
        response.setDeptName(deptName);
        response.setStatus(user.getStatus());
        response.setToken(generateToken(user.getId(), user.getUserCode()));
        
        saveLoginLog(user.getId(), user.getUserCode(), httpRequest, 1, "登录成功", user.getUsername());
        saveOperationLog(user.getId(), user.getUserCode(), user.getUsername(), httpRequest, "系统登录", "无");
        
        user.setLastLoginTime(new Date());
        user.setLastLoginIp(getClientIp(httpRequest));
        userMapper.update(user);
        
        redisService.set("user:token:" + response.getToken(), response, 24, TimeUnit.HOURS);
        redisService.set("user:info:" + userCode, response, 24, TimeUnit.HOURS);
        logger.info("登录成功 - 用户: {} ({})", user.getRealName(), userCode);
        
        return JsonResult.success(response, "登录成功");
    }

    private String generateToken(Long userId, String userCode) {
        return "TOKEN_" + userId + "_" + userCode + "_" + System.currentTimeMillis();
    }

    private void saveLoginLog(Long userId, String userCode, HttpServletRequest request, Integer status, String msg, String username) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUserCode(userCode);
        log.setUsername(username);
        log.setLoginType(1);
        log.setLoginIp(getClientIp(request));
        String userAgent = request.getHeader("User-Agent");
        log.setBrowser(userAgent != null && userAgent.length() > 50 ? userAgent.substring(0, 50) : userAgent);
        log.setOs(userAgent != null && userAgent.length() > 50 ? userAgent.substring(0, 50) : userAgent);
        log.setStatus(status);
        log.setMsg(msg);
        log.setLoginTime(new Date());
        loginLogMapper.insert(log);
    }

    private void saveOperationLog(Long userId, String userCode, String username, HttpServletRequest request, String module, String operation) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUserCode(userCode);
        log.setUsername(username);
        log.setModule(module);
        log.setOperation(operation);
        log.setMethod(request.getMethod());
        log.setRequestUrl(request.getRequestURI());
        log.setOperateIp(getClientIp(request));
        log.setOperateTime(new Date());
        log.setStatus(1);
        operationLogMapper.insert(log);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @PostMapping("/logout")
    public JsonResult<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            redisService.delete("user:token:" + token);
            logger.info("用户退出登录 - Token已清除");
        }
        return JsonResult.success(null, "退出成功");
    }
}

