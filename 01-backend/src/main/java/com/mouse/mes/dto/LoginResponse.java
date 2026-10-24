package com.mouse.mes.dto;

import lombok.Data;

import java.util.Date;

@Data
public class LoginResponse {
    private Long id;
    private String userCode;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Integer gender;
    private String avatar;
    private Long roleId;
    private String roleName;
    private Long deptId;
    private String deptName;
    private Integer status;
    private String token;
}
