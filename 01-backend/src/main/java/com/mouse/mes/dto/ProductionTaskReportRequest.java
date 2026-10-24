package com.mouse.mes.dto;

import lombok.Data;

/**
 * 操作工提交生产报工时使用的请求数据。
 */
@Data
public class ProductionTaskReportRequest {

    /** 当前报工人的员工ID。 */
    private Long userId;

    /** 当前报工人的员工姓名。 */
    private String userName;

    /** 本次实际完成的产品数量，尚未经过质检判定。 */
    private Integer reportQuantity;

    /** 本次报工的生产说明。 */
    private String remark;
}
