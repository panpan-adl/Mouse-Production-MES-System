package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class StockIn {
    /**
     * 入库单ID，数据库主键。
     */
    private Long id;

    /**
     * 入库单号。
     */
    private String stockInNo;

    /**
     * 入库类型：PURCHASE采购入库、PRODUCTION生产入库、OTHER其他入库。
     */
    private String stockInType;

    /**
     * 入库仓库ID。
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
     * 入库总数量。
     */
    private BigDecimal totalQuantity;

    /**
     * 入库状态。
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
     * 入库明细列表，一个入库单可包含多种物料。
     */
    private List<StockInItem> items;
}
