package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Department {
    private Long id;
    private String roleCode;        
    private Long parentId;
    private String deptName;
    private String deptCode;
    private Long leaderId;
    private String leaderName;
    private Integer sort;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
