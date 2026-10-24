package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QcInspectionItem {
    /**
     * 质检明细ID，数据库主键。
     */
    private Long id;

    /**
     * 所属质检单ID。
     */
    private Long inspectionId;

    /**
     * 被检物料ID。
     */
    private Long materialId;

    /**
     * 批次ID。
     */
    private Long lotId;

    /**
     * 检测项名称，例如外观、尺寸、按键寿命。
     */
    private String inspectionItem;

    /**
     * 检测标准，例如无划痕、误差小于0.1mm。
     */
    private String inspectionStandard;

    /**
     * 实测值或检测描述。
     */
    private String actualValue;

    /**
     * 该检测项检验数量。
     */
    private BigDecimal sampleQuantity;

    /**
     * 该检测项合格数量。
     */
    private BigDecimal qualifiedQuantity;

    /**
     * 该检测项不合格数量。
     */
    private BigDecimal defectiveQuantity;

    /**
     * 该检测项结果。
     */
    private String itemResult;

    /**
     * 缺陷类型。
     */
    private String defectType;

    /**
     * 不合格原因。
     */
    private String defectReason;

    /**
     * 处理方式。
     */
    private String handleMethod;

    /**
     * 明细备注。
     */
    private String remark;
}
