package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Supplier {
    private Long id;
    private String supplierCode;
    private String supplierName;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String bankName;
    private String bankAccount;
    private Integer status;
    private String remark;
    private Date createTime;
    private Date updateTime;
}