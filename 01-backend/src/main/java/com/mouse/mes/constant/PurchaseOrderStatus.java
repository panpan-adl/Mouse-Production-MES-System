package com.mouse.mes.constant;

/**
 * 采购订单状态常量，统一管理采购订单从草稿到完成/取消的状态值。
 */
public final class PurchaseOrderStatus {

    /**
     * 草稿状态：采购订单刚创建，还未提交审核。
     */
    public static final String DRAFT = "DRAFT";

    /**
     * 已提交状态：采购订单已提交，等待审核。
     */
    public static final String SUBMITTED = "SUBMITTED";

    /**
     * 已审核状态：采购订单已审核通过，可以进行采购到货。
     */
    public static final String APPROVED = "APPROVED";

    /**
     * 部分到货状态：采购订单中至少有一部分物料已经到货，但未全部到货。
     */
    public static final String PARTIAL_RECEIVED = "PARTIAL_RECEIVED";

    /**
     * 全部到货状态：采购订单下所有明细均已完成到货。
     */
    public static final String COMPLETED = "COMPLETED";

    /**
     * 已取消状态：采购订单已取消，不能再进行到货。
     */
    public static final String CANCELLED = "CANCELLED";

    /**
     * 私有构造函数，防止工具类被实例化。
     */
    private PurchaseOrderStatus() {
    }

    /**
     * 判断传入状态是否属于采购订单允许的状态。
     */
    public static boolean isValid(String status) {
        return DRAFT.equals(status)
                || SUBMITTED.equals(status)
                || APPROVED.equals(status)
                || PARTIAL_RECEIVED.equals(status)
                || COMPLETED.equals(status)
                || CANCELLED.equals(status);
    }
}
