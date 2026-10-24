package com.mouse.mes.constant;

/**
 * 质检类型常量，区分来料、生产过程和成品质检。
 */
public final class QcInspectionType {

    /**
     * 来料质检。
     */
    public static final String INCOMING = "INCOMING";

    /**
     * 生产质检。
     */
    public static final String PRODUCTION = "PRODUCTION";

    /**
     * 成品质检。
     */
    public static final String FINISHED = "FINISHED";

    /**
     * 私有构造函数，防止工具类被实例化。
     */
    private QcInspectionType() {
    }

    /**
     * 判断质检类型是否合法。
     */
    public static boolean isValid(String inspectionType) {
        return INCOMING.equals(inspectionType)
                || PRODUCTION.equals(inspectionType)
                || FINISHED.equals(inspectionType);
    }
}
