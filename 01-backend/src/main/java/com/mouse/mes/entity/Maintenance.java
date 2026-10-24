package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Maintenance {
    /**
     * 维修单ID，数据库主键。
     */
    private Long id;

    /**
     * 维修单号。
     */
    private String maintenanceNo;

    /**
     * 设备ID。
     */
    private Long equipmentId;

    /**
     * 设备名称。
     */
    private String equipmentName;

    /**
     * 维修类型，保留数字类型便于兼容原表：1保养、2维修、3巡检。
     */
    private Integer maintenanceType;

    /**
     * 故障描述。
     */
    private String faultDesc;

    /**
     * 维修内容。
     */
    private String maintenanceContent;

    /**
     * 维修人员ID。
     */
    private Long maintenanceUserId;

    /**
     * 维修人员姓名。
     */
    private String maintenanceUserName;

    /**
     * 维修开始时间。
     */
    private Date startTime;

    /**
     * 维修结束时间。
     */
    private Date endTime;

    /**
     * 维修费用。
     */
    private BigDecimal cost;

    /**
     * 维修结果，例如REPAIRED、SCRAPPED或文字说明。
     */
    private String result;

    /**
     * 维修单状态：PENDING、PROCESSING、FINISHED、CANCELLED。
     */
    private String status;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;
}
