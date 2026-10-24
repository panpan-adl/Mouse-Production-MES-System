package com.mouse.mes.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Equipment {
    /**
     * 设备ID，数据库主键。
     */
    private Long id;

    /**
     * 设备编号，用于业务查询和对外展示。
     */
    private String equipmentCode;

    /**
     * 设备名称。
     */
    private String equipmentName;

    /**
     * 设备类型。
     */
    private String equipmentType;

    /**
     * 设备型号。
     */
    private String model;

    /**
     * 生产厂商。
     */
    private String manufacturer;

    /**
     * 购置日期。
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date purchaseDate;

    /**
     * 购置价格。
     */
    private BigDecimal purchasePrice;

    /**
     * 保修期，单位为月。
     */
    private Integer warrantyPeriod;

    /**
     * 设备位置。
     */
    private String location;

    /**
     * 设备状态：NORMAL（正常）、REPAIR_REQUESTED（报修中）、REPAIRING（维修中）、REPAIRED（已维修）、SCRAPPED（报废）。
     */
    private String status;

    /**
     * 负责人ID。
     */
    private Long responsibleId;

    /**
     * 负责人姓名。
     */
    private String responsibleName;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 创建时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * 逻辑删除标记，0未删除，1已删除。
     */
    private Integer isDeleted;
}
