package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Role {
    private Integer id;
    private String roleCode;
    private String roleName;
    private String roleDesc;
    private Integer sort;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}