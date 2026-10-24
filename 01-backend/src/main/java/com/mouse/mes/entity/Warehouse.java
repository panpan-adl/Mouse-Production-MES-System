package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Warehouse {
    private Long id;
    private String warehouseCode;
    private String warehouseName;
    private String location;
    private Long managerId;
    private String managerName;
    private Integer status;
    private String remark;
    private Date createTime;
    private Date updateTime;
}