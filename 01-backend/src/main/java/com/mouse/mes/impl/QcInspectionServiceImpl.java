package com.mouse.mes.impl;

import com.mouse.mes.constant.QcInspectionResult;
import com.mouse.mes.constant.QcInspectionType;
import com.mouse.mes.entity.QcInspection;
import com.mouse.mes.entity.QcInspectionItem;
import com.mouse.mes.mapper.QcInspectionItemMapper;
import com.mouse.mes.mapper.QcInspectionMapper;
import com.mouse.mes.service.QcInspectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
public class QcInspectionServiceImpl implements QcInspectionService {

    /**
     * 质检单数据库访问对象。
     */
    @Autowired
    private QcInspectionMapper qcInspectionMapper;

    /**
     * 质检明细数据库访问对象。
     */
    @Autowired
    private QcInspectionItemMapper qcInspectionItemMapper;

    /**
     * 查询全部未删除质检单，并填充明细。
     */
    @Override
    public List<QcInspection> findAll() {
        List<QcInspection> inspections = qcInspectionMapper.findAll();
        for (QcInspection inspection : inspections) {
            fillItems(inspection);
        }
        return inspections;
    }

    /**
     * 按质检单ID查询质检单，并填充明细。
     */
    @Override
    public QcInspection findById(Long id) {
        QcInspection inspection = qcInspectionMapper.findById(id);
        fillItems(inspection);
        return inspection;
    }

    /**
     * 按质检单号查询质检单，并填充明细。
     */
    @Override
    public QcInspection findByInspectionNo(String inspectionNo) {
        QcInspection inspection = qcInspectionMapper.findByInspectionNo(inspectionNo);
        fillItems(inspection);
        return inspection;
    }

    /**
     * 按质检类型查询质检单，并填充明细。
     */
    @Override
    public List<QcInspection> findByInspectionType(String inspectionType) {
        List<QcInspection> inspections = qcInspectionMapper.findByInspectionType(inspectionType);
        for (QcInspection inspection : inspections) {
            fillItems(inspection);
        }
        return inspections;
    }

    /**
     * 按质检结果查询质检单，并填充明细。
     */
    @Override
    public List<QcInspection> findByResult(String result) {
        List<QcInspection> inspections = qcInspectionMapper.findByResult(result);
        for (QcInspection inspection : inspections) {
            fillItems(inspection);
        }
        return inspections;
    }

    /**
     * 新增质检单和质检明细。
     */
    @Override
    @Transactional
    public QcInspection create(QcInspection qcInspection) {
        validateBase(qcInspection);
        if (qcInspectionMapper.findByInspectionNo(qcInspection.getInspectionNo()) != null) {
            throw new IllegalArgumentException("质检单号已存在");
        }
        normalizeInspection(qcInspection);
        qcInspection.setCreatedAt(new Date());
        qcInspection.setUpdatedAt(new Date());
        qcInspection.setIsDeleted(0);
        qcInspectionMapper.insert(qcInspection);
        saveItems(qcInspection);
        return findById(qcInspection.getId());
    }

    /**
     * 修改质检单和质检明细。
     */
    @Override
    @Transactional
    public QcInspection update(QcInspection qcInspection) {
        QcInspection existing = qcInspectionMapper.findById(qcInspection.getId());
        if (existing == null) {
            throw new IllegalArgumentException("质检单不存在");
        }
        mergeMissingFields(qcInspection, existing);
        normalizeInspection(qcInspection);
        qcInspection.setUpdatedAt(new Date());
        qcInspectionMapper.update(qcInspection);
        if (qcInspection.getItems() != null) {
            qcInspectionItemMapper.deleteByInspectionId(qcInspection.getId());
            saveItems(qcInspection);
        }
        return findById(qcInspection.getId());
    }

    /**
     * 逻辑删除质检单。
     */
    @Override
    public void delete(Long id) {
        QcInspection existing = qcInspectionMapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("质检单不存在");
        }
        qcInspectionMapper.delete(id);
    }

    /**
     * 给质检单填充明细。
     */
    private void fillItems(QcInspection inspection) {
        if (inspection != null) {
            inspection.setItems(qcInspectionItemMapper.findByInspectionId(inspection.getId()));
        }
    }

    /**
     * 校验质检单基础字段。
     */
    private void validateBase(QcInspection qcInspection) {
        if (qcInspection == null) {
            throw new IllegalArgumentException("质检单不能为空");
        }
        if (qcInspection.getInspectionNo() == null || qcInspection.getInspectionNo().isBlank()) {
            throw new IllegalArgumentException("质检单号不能为空");
        }
        if (qcInspection.getInspectionType() == null || qcInspection.getInspectionType().isBlank()) {
            throw new IllegalArgumentException("质检类型不能为空");
        }
        if (!QcInspectionType.isValid(qcInspection.getInspectionType())) {
            throw new IllegalArgumentException("质检类型不合法");
        }
    }

    /**
     * 标准化质检单数量、结果、合格率和业务状态。
     */
    private void normalizeInspection(QcInspection qcInspection) {
        validateBase(qcInspection);
        if (qcInspection.getItems() != null && !qcInspection.getItems().isEmpty()) {
            aggregateFromItems(qcInspection);
        } else {
            normalizeHeaderQuantity(qcInspection);
        }
        if (qcInspection.getResult() == null || qcInspection.getResult().isBlank()) {
            qcInspection.setResult(qcInspection.getDefectiveQuantity().compareTo(BigDecimal.ZERO) == 0
                    ? QcInspectionResult.QUALIFIED : QcInspectionResult.UNQUALIFIED);
        }
        if (!QcInspectionResult.isValid(qcInspection.getResult())) {
            throw new IllegalArgumentException("质检结果不合法");
        }
        if (QcInspectionResult.needsDefectHandling(qcInspection.getResult()) && !hasDefectHandling(qcInspection)) {
            throw new IllegalArgumentException("不合格时必须记录原因和处理方式");
        }
        qcInspection.setBusinessStatus(resolveBusinessStatus(qcInspection));
    }

    /**
     * 从质检明细汇总主单数量。
     */
    private void aggregateFromItems(QcInspection qcInspection) {
        BigDecimal sampleTotal = BigDecimal.ZERO;
        BigDecimal qualifiedTotal = BigDecimal.ZERO;
        for (QcInspectionItem item : qcInspection.getItems()) {
            normalizeItem(item);
            sampleTotal = sampleTotal.add(item.getSampleQuantity());
            qualifiedTotal = qualifiedTotal.add(item.getQualifiedQuantity());
        }
        qcInspection.setSampleQuantity(sampleTotal);
        qcInspection.setQualifiedQuantity(qualifiedTotal);
        qcInspection.setDefectiveQuantity(sampleTotal.subtract(qualifiedTotal));
        qcInspection.setQualifiedRate(calculateQualifiedRate(sampleTotal, qualifiedTotal));
    }

    /**
     * 使用主单数量计算不合格数量和合格率。
     */
    private void normalizeHeaderQuantity(QcInspection qcInspection) {
        BigDecimal sampleQuantity = safe(qcInspection.getSampleQuantity());
        BigDecimal qualifiedQuantity = safe(qcInspection.getQualifiedQuantity());
        if (sampleQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("检验数量必须大于0");
        }
        if (qualifiedQuantity.compareTo(sampleQuantity) > 0) {
            throw new IllegalArgumentException("合格数量不能大于检验数量");
        }
        qcInspection.setSampleQuantity(sampleQuantity);
        qcInspection.setQualifiedQuantity(qualifiedQuantity);
        qcInspection.setDefectiveQuantity(sampleQuantity.subtract(qualifiedQuantity));
        qcInspection.setQualifiedRate(calculateQualifiedRate(sampleQuantity, qualifiedQuantity));
    }

    /**
     * 标准化并校验质检明细。
     */
    private void normalizeItem(QcInspectionItem item) {
        if (item == null) {
            throw new IllegalArgumentException("质检明细不能为空");
        }
        if (item.getInspectionItem() == null || item.getInspectionItem().isBlank()) {
            throw new IllegalArgumentException("检测项不能为空");
        }
        BigDecimal sampleQuantity = safe(item.getSampleQuantity());
        BigDecimal qualifiedQuantity = safe(item.getQualifiedQuantity());
        if (sampleQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("检验数量必须大于0");
        }
        if (qualifiedQuantity.compareTo(sampleQuantity) > 0) {
            throw new IllegalArgumentException("合格数量不能大于检验数量");
        }
        item.setSampleQuantity(sampleQuantity);
        item.setQualifiedQuantity(qualifiedQuantity);
        item.setDefectiveQuantity(sampleQuantity.subtract(qualifiedQuantity));
        if (item.getItemResult() == null || item.getItemResult().isBlank()) {
            item.setItemResult(item.getDefectiveQuantity().compareTo(BigDecimal.ZERO) == 0
                    ? QcInspectionResult.QUALIFIED : QcInspectionResult.UNQUALIFIED);
        }
        if (!QcInspectionResult.isValid(item.getItemResult())) {
            throw new IllegalArgumentException("质检明细结果不合法");
        }
    }

    /**
     * 保存质检明细。
     */
    private void saveItems(QcInspection qcInspection) {
        if (qcInspection.getItems() == null) {
            return;
        }
        for (QcInspectionItem item : qcInspection.getItems()) {
            item.setInspectionId(qcInspection.getId());
            normalizeItem(item);
            qcInspectionItemMapper.insert(item);
        }
    }

    /**
     * 计算合格率，保留两位小数。
     */
    private BigDecimal calculateQualifiedRate(BigDecimal sampleQuantity, BigDecimal qualifiedQuantity) {
        if (sampleQuantity == null || sampleQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return qualifiedQuantity.multiply(new BigDecimal("100")).divide(sampleQuantity, 2, RoundingMode.HALF_UP);
    }

    /**
     * 判断质检单或质检明细是否记录了不合格原因和处理方式。
     */
    private boolean hasDefectHandling(QcInspection qcInspection) {
        if (hasText(qcInspection.getDefectReason()) && hasText(qcInspection.getHandleMethod())) {
            return true;
        }
        if (qcInspection.getItems() == null) {
            return false;
        }
        for (QcInspectionItem item : qcInspection.getItems()) {
            if (hasText(item.getDefectReason()) && hasText(item.getHandleMethod())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据质检类型和质检结果得到后续业务状态。
     */
    private String resolveBusinessStatus(QcInspection qcInspection) {
        if (!QcInspectionResult.isPassLike(qcInspection.getResult())) {
            if (QcInspectionResult.REWORK.equals(qcInspection.getResult())) {
                return "NEED_REWORK";
            }
            if (QcInspectionResult.SCRAP.equals(qcInspection.getResult())) {
                return "SCRAPPED";
            }
            return "BLOCKED";
        }
        if (QcInspectionType.INCOMING.equals(qcInspection.getInspectionType())) {
            return "ALLOW_STOCK_IN";
        }
        if (QcInspectionType.PRODUCTION.equals(qcInspection.getInspectionType())) {
            return "ALLOW_NEXT_STEP";
        }
        if (QcInspectionType.FINISHED.equals(qcInspection.getInspectionType())) {
            return "ALLOW_FINISHED_STOCK_IN";
        }
        return "PASSED";
    }

    /**
     * 修改质检单时补齐未传字段，避免旧数据被空值覆盖。
     */
    private void mergeMissingFields(QcInspection target, QcInspection existing) {
        if (target.getInspectionNo() == null || target.getInspectionNo().isBlank()) {
            target.setInspectionNo(existing.getInspectionNo());
        }
        if (target.getInspectionType() == null || target.getInspectionType().isBlank()) {
            target.setInspectionType(existing.getInspectionType());
        }
        if (target.getSourceOrderId() == null) {
            target.setSourceOrderId(existing.getSourceOrderId());
        }
        if (target.getSourceOrderNo() == null) {
            target.setSourceOrderNo(existing.getSourceOrderNo());
        }
        if (target.getSampleQuantity() == null) {
            target.setSampleQuantity(existing.getSampleQuantity());
        }
        if (target.getQualifiedQuantity() == null) {
            target.setQualifiedQuantity(existing.getQualifiedQuantity());
        }
        if (target.getResult() == null || target.getResult().isBlank()) {
            target.setResult(existing.getResult());
        }
        if (target.getDefectReason() == null) {
            target.setDefectReason(existing.getDefectReason());
        }
        if (target.getHandleMethod() == null) {
            target.setHandleMethod(existing.getHandleMethod());
        }
        if (target.getBusinessStatus() == null) {
            target.setBusinessStatus(existing.getBusinessStatus());
        }
        if (target.getRemark() == null) {
            target.setRemark(existing.getRemark());
        }
        if (target.getCreatedBy() == null) {
            target.setCreatedBy(existing.getCreatedBy());
        }
        target.setCreatedAt(existing.getCreatedAt());
        target.setIsDeleted(existing.getIsDeleted());
    }

    /**
     * 判断字符串是否有实际内容。
     */
    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    /**
     * 将空数量转换为0。
     */
    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
