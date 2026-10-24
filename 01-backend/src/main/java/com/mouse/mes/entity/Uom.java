package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Uom {
    private Long id;
    private String uomCode;
    private String uomName;
    private String status;
    private Date createdAt;
    private Date updatedAt;
    private Integer isDeleted;
}
