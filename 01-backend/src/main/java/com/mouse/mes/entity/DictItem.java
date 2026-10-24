package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class DictItem {
    private Long id;
    private String dictType;
    private String itemLabel;
    private String itemValue;
    private Integer sort;
    private Integer status;
    private String remark;
    private Date createTime;
    private Date updateTime;
}