package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class PurchaseOrder {
    /**
     * 采购订单ID，数据库主键。
     */
    private Long id;

    /**
     * 采购订单编号，用于业务查询和对外展示。
     */
    private String poNo;

    /**
     * 供应商ID，关联供应商表。
     */
    private Long supplierId;

    /**
     * 关联的销售订单ID。
     */
    private Long salesOrderId;

    /**
     * 下单日期。
     */
    private Date orderDate;

    /**
     * 预计到货日期。
     */
    private Date expectedDate;

    /**
     * 采购订单总金额。
     */
    private BigDecimal totalAmount;

    /**
     * 采购订单状态，例如 DRAFT、SUBMITTED、APPROVED、PARTIAL_RECEIVED、COMPLETED、CANCELLED。
     */
    private String status;

    /**
     * 采购订单备注。
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
     * 采购订单明细列表，一个采购订单可以包含多个物料明细。
     */
    private List<PurchaseOrderItem> items;
}
