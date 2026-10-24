package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Attendance {
    private Long id;
    private Long userId;
    private String userName;
    private Date attendanceDate;
    private Date checkInTime;
    private Date checkOutTime;
    private BigDecimal workHours;
    private Integer status;
    private String remark;
    private Date createTime;
}