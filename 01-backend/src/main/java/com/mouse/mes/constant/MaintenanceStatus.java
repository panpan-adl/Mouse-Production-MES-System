package com.mouse.mes.constant;

/**
 * 维修单状态常量。
 */
public final class MaintenanceStatus {

    /**
     * 待维修。
     */
    public static final String PENDING = "PENDING";

    /**
     * 维修中。
     */
    public static final String PROCESSING = "PROCESSING";

    /**
     * 已完成。
     */
    public static final String FINISHED = "FINISHED";

    /**
     * 已取消。
     */
    public static final String CANCELLED = "CANCELLED";

    /**
     * 私有构造函数，防止工具类被实例化。
     */
    private MaintenanceStatus() {
    }

    /**
     * 判断维修单状态是否合法。
     */
    public static boolean isValid(String status) {
        return PENDING.equals(status)
                || PROCESSING.equals(status)
                || FINISHED.equals(status)
                || CANCELLED.equals(status);
    }

    /**
     * 判断维修单是否仍未完成。
     */
    public static boolean isUnfinished(String status) {
        return PENDING.equals(status) || PROCESSING.equals(status);
    }
}
