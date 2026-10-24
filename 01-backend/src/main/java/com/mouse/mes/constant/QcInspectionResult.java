package com.mouse.mes.constant;

/**
 * 质检结果常量，统一保存质检完成后的结果值。
 */
public final class QcInspectionResult {

    /**
     * 合格。
     */
    public static final String QUALIFIED = "QUALIFIED";

    /**
     * 不合格。
     */
    public static final String UNQUALIFIED = "UNQUALIFIED";

    /**
     * 让步接收。
     */
    public static final String CONCESSION_ACCEPT = "CONCESSION_ACCEPT";

    /**
     * 返工。
     */
    public static final String REWORK = "REWORK";

    /**
     * 报废。
     */
    public static final String SCRAP = "SCRAP";

    /**
     * 私有构造函数，防止工具类被实例化。
     */
    private QcInspectionResult() {
    }

    /**
     * 判断质检结果是否合法。
     */
    public static boolean isValid(String result) {
        return QUALIFIED.equals(result)
                || UNQUALIFIED.equals(result)
                || CONCESSION_ACCEPT.equals(result)
                || REWORK.equals(result)
                || SCRAP.equals(result);
    }

    /**
     * 判断质检结果是否允许业务继续流转。
     */
    public static boolean isPassLike(String result) {
        return QUALIFIED.equals(result) || CONCESSION_ACCEPT.equals(result);
    }

    /**
     * 判断质检结果是否需要记录不合格原因和处理方式。
     */
    public static boolean needsDefectHandling(String result) {
        return UNQUALIFIED.equals(result) || REWORK.equals(result) || SCRAP.equals(result);
    }
}
