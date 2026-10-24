package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class LoginLog {
    private Long id;
    private Long userId;
    private String userCode;
    private String username;
    private Integer loginType;
    private String loginIp;
    private String loginLocation;
    private String browser;
    private String os;
    private Integer status;
    private String msg;
    private Date loginTime;
}