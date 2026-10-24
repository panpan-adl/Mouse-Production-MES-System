package com.mouse.mes.dto;

import com.mouse.mes.entity.PurchaseOrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 采购订单新增和修改请求对象，用于同时接收订单主表信息和多个采购明细。
 */
@Data
public class PurchaseOrderRequest {

    /**
     * 采购订单编号。
     */
    private String poNo;

    /**
     * 供应商ID。
     */
    private Long supplierId;

    /**
     * 下单日期。
     */
    private Date orderDate;

    /**
     * 预计到货日期。
     */
    private Date expectedDate;

    /**
     * 采购订单总金额，如果为空则由采购明细自动计算。
     */
    private BigDecimal totalAmount;

    /**
     * 采购订单状态，如果为空则默认 DRAFT。
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
     * 采购订单明细列表，一个采购订单可携带多个物料明细。
     */
    private List<PurchaseOrderItem> items;
}
