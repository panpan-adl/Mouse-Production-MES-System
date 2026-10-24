package com.mouse.mes.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 采购到货请求对象，用于接收本次到货的仓库、操作人和明细数量。
 */
@Data
public class PurchaseReceiveRequest {

    /**
     * 到货入库的仓库ID。
     */
    private Long warehouseId;

    /**
     * 到货操作人ID。
     */
    private Long createdBy;

    /**
     * 到货备注。
     */
    private String remark;

    /**
     * 本次到货明细列表。
     */
    private List<Item> items;

    /**
     * 单行采购到货明细。
     */
    @Data
    public static class Item {

        /**
         * 采购订单明细ID，用于定位是哪一行物料到货。
         */
        private Long purchaseOrderItemId;

        /**
         * 本次到货数量。
         */
        private BigDecimal receiveQuantity;

        /**
         * 批次ID，可以为空，由仓库后续补充也可以。
         */
        private Long lotId;

        /**
         * 库位ID，可以为空，由仓库后续补充也可以。
         */
        private Long locationId;

        /**
         * 单行到货备注。
         */
        private String remark;
    }
}
