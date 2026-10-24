package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class OperationLog {
    private Long id;
    private Long userId;
    private String userCode;
    private String username;
    private String module;
    private String operation;
    private String method;
    private String requestUrl;
    private String requestParam;
    private String responseResult;
    private String operateIp;
    private Date operateTime;
    private Integer status;
    private String errorMsg;
}