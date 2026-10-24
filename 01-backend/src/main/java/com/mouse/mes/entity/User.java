package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class User {
    private Long id;
    private String userCode;
    private String username;
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Integer gender;
    private String avatar;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private Long deptId;
    private String deptName;
    private Integer status;
    private Date entryDate;
    private Date leaveDate;
    private Date lastLoginTime;
    private String lastLoginIp;
    private Date createTime;
    private Date updateTime;
    private Integer deleted;
}
