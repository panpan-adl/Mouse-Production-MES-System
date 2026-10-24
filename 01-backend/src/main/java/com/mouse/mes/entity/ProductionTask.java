package com.mouse.mes.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ProductionTask {
    /** 待领取：尚未分配给操作工。 */
    public static final int STATUS_PENDING_CLAIM = 0;

    /** 已领取：任务已归属某位操作工，尚未开工。 */
    public static final int STATUS_CLAIMED = 1;

    /** 生产中：操作工已开始执行任务。 */
    public static final int STATUS_IN_PROGRESS = 2;

    /** 待质检：任务数量已经报工完成，等待质检员判定。 */
    public static final int STATUS_PENDING_QC = 3;

    /** 已完成：质检和后续业务均已完成。 */
    public static final int STATUS_COMPLETED = 4;

    /** 已取消：任务不再继续执行。 */
    public static final int STATUS_CANCELLED = 5;

    private Long id;
    private String taskNo;
    private Long planId;
    private Integer taskType;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Long assignedUserId;
    private String assignedUserName;
    private Integer taskStatus;
    private Date startTime;
    private Date endTime;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
