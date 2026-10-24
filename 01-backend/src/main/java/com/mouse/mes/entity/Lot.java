package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Lot {
    private Long id;
    private Long materialId;
    private String lotNo;
    private Date productionDate;
    private Date expireDate;
    private Date createdAt;
    private Date updatedAt;
    private Integer isDeleted;
}
