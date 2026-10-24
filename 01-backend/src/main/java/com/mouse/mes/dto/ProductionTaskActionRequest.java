package com.mouse.mes.dto;

import lombok.Data;

/**
 * 生产任务领取和开工时使用的操作人信息。
 */
@Data
public class ProductionTaskActionRequest {

    /** 当前执行该操作的员工ID。 */
    private Long userId;

    /** 当前执行该操作的员工姓名，用于任务执行记录展示。 */
    private String userName;
}
