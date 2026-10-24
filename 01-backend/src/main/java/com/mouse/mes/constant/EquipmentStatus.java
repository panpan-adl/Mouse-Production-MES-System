package com.mouse.mes.constant;

/**
 * 设备状态常量。
 */
public final class EquipmentStatus {

    /**
     * 正常。
     */
    public static final String NORMAL = "NORMAL";

    /**
     * 报修中。
     */
    public static final String REPAIR_REQUESTED = "REPAIR_REQUESTED";

    /**
     * 维修中。
     */
    public static final String REPAIRING = "REPAIRING";

    /**
     * 已维修。
     */
    public static final String REPAIRED = "REPAIRED";

    /**
     * 报废。
     */
    public static final String SCRAPPED = "SCRAPPED";

    /**
     * 私有构造函数，防止工具类被实例化。
     */
    private EquipmentStatus() {
    }

    /**
     * 判断设备状态是否合法。
     */
    public static boolean isValid(String status) {
        return NORMAL.equals(status)
                || REPAIR_REQUESTED.equals(status)
                || REPAIRING.equals(status)
                || REPAIRED.equals(status)
                || SCRAPPED.equals(status);
    }
}
