package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class UserRole {
    private Long id;
    private Long userId;
    private String roleCode;
    private Date createdAt;
}
