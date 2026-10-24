package com.mouse.mes.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class QcInspection {
    /**
     * 质检单ID，数据库主键。
     */
    private Long id;

    /**
     * 质检单号，用于业务查询和对外展示。
     */
    private String inspectionNo;

    /**
     * 质检类型：INCOMING来料质检、PRODUCTION生产质检、FINISHED成品质检。
     */
    private String inspectionType;

    /**
     * 来源业务单据ID，例如采购订单、生产任务或成品入库来源。
     */
    private Long sourceOrderId;

    /**
     * 来源业务单据编号。
     */
    private String sourceOrderNo;

    /**
     * 检验总数量。
     */
    private BigDecimal sampleQuantity;

    /**
     * 合格数量。
     */
    private BigDecimal qualifiedQuantity;

    /**
     * 不合格数量，由检验总数量减去合格数量得到。
     */
    private BigDecimal defectiveQuantity;

    /**
     * 合格率，单位为百分比，例如95.50表示95.50%。
     */
    private BigDecimal qualifiedRate;

    /**
     * 质检结果：QUALIFIED、UNQUALIFIED、CONCESSION_ACCEPT、REWORK、SCRAP。
     */
    private String result;

    /**
     * 不合格原因，不合格、返工、报废时需要记录。
     */
    private String defectReason;

    /**
     * 处理方式，不合格、返工、报废时需要记录。
     */
    private String handleMethod;

    /**
     * 质检完成后的业务放行状态，例如ALLOW_STOCK_IN、ALLOW_NEXT_STEP、BLOCKED。
     */
    private String businessStatus;

    /**
     * 质检备注。
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
     * 逻辑删除标记，0未删除，1已删除。
     */
    private Integer isDeleted;

    /**
     * 质检单明细，一个质检单可以包含多个检测项。
     */
    private List<QcInspectionItem> items;
}
