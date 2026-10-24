package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Dict {
    private Long id;
    private String dictName;
    private String dictType;
    private Integer status;
    private String remark;
    private Date createTime;
    private Date updateTime;
}