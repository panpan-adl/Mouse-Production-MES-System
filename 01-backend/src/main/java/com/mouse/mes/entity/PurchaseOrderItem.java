package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderItem {
    /**
     * 采购订单明细ID，数据库主键。
     */
    private Long id;

    /**
     * 所属采购订单ID。
     */
    private Long poId;

    /**
     * 采购物料ID。
     */
    private Long materialId;

    /**
     * 采购数量。
     */
    private BigDecimal quantity;

    /**
     * 采购单价。
     */
    private BigDecimal unitPrice;

    /**
     * 本行采购金额。
     */
    private BigDecimal amount;

    /**
     * 已到货数量，用于判断部分到货、全部到货和超量到货。
     */
    private BigDecimal receivedQuantity;

    /**
     * 明细备注。
     */
    private String remark;
}
