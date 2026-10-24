package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Material {
    private Long id;
    private String materialCode;
    private String materialName;
    private Integer materialType;
    private String specification;
    private String unit;
    private Integer safetyStock;
    private Integer currentStock;
    private Integer status;
    private String remark;
    private Date createTime;
    private Date updateTime;
}