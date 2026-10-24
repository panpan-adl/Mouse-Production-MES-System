package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Location {
    private Long id;
    private Long warehouseId;
    private String locationCode;
    private String locationName;
    private String locationType;
    private String status;
    private Date createdAt;
    private Date updatedAt;
    private Integer isDeleted;
}
