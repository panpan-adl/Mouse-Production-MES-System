package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Customer {
    private Long id;
    private String customerCode;
    private String customerName;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private String status;
    private Date createdAt;
    private Date updatedAt;
    private Integer isDeleted;
}
