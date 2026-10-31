package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class MaterialRequest {
    private Long purchaseOrderId;
    private String purchaseOrderNo;
    /** 物料请求ID。 */
    private Long id;

    /** 物料请求编号，用于前端和采购流程追踪。 */
    private String requestNo;

    /** 来源生产任务ID。 */
    private Long sourceTaskId;
    private java.time.LocalDate requiredDate;

    /** 来源生产任务编号。 */
    private String sourceTaskNo;

    /** 来源任务对应的产品名称。 */
    private String productName;

    /** 需求物料ID。 */
    private Long materialId;

    /** 需求物料编码，创建和刷新时从物料主数据带出。 */
    private String materialCode;

    /** 需求物料名称，创建和刷新时从物料主数据带出。 */
    private String materialName;

    /** 生产任务需要的物料数量。 */
    private BigDecimal requiredQuantity;

    /** 当前可用库存数量。 */
    private BigDecimal availableQuantity;

    /** 缺料数量，等于需求数量减可用库存，小于0时按0处理。 */
    private BigDecimal shortageQuantity;

    /** 建议出料仓库ID。 */
    private Long warehouseId;

    /** 请求状态：PENDING_CHECK、ENOUGH、SHORTAGE、PURCHASE_REQUESTED、PURCHASE_ORDERED。 */
    private String requestStatus;

    /** 业务说明。 */
    private String remark;

    /** 创建人ID。 */
    private Long createdBy;

    /** 创建时间。 */
    private Date createdAt;

    /** 更新时间。 */
    private Date updatedAt;

    /** 逻辑删除标记。 */
    private Integer isDeleted;
}
