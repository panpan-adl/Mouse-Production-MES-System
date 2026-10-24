package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class StockOut {
    /**
     * 出库单ID，数据库主键。
     */
    private Long id;

    /**
     * 出库单号。
     */
    private String stockOutNo;

    /**
     * 出库类型：PRODUCTION_ISSUE生产领料出库、SALES销售出库、OTHER其他出库。
     */
    private String stockOutType;

    /**
     * 出库仓库ID。
     */
    private Long warehouseId;

    /**
     * 来源单据ID。
     */
    private Long sourceOrderId;

    /**
     * 来源单据编号。
     */
    private String sourceOrderNo;

    /**
     * 出库总数量。
     */
    private BigDecimal totalQuantity;

    /**
     * 出库状态。
     */
    private String status;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 创建人ID。
     */
    private Long createdBy;

    /**
     * 创建时间。
     */
    private Date createdAt;

    /**
     * 更新时间。
     */
    private Date updatedAt;

    /**
     * 逻辑删除标记：0未删除，1已删除。
     */
    private Integer isDeleted;

    /**
     * 出库明细列表，一个出库单可包含多种物料。
     */
    private List<StockOutItem> items;
}
