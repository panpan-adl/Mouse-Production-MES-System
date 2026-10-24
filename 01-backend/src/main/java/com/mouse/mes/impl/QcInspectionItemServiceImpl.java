package com.mouse.mes.impl;

import com.mouse.mes.constant.QcInspectionResult;
import com.mouse.mes.entity.QcInspectionItem;
import com.mouse.mes.mapper.QcInspectionItemMapper;
import com.mouse.mes.service.QcInspectionItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class QcInspectionItemServiceImpl implements QcInspectionItemService {

    /**
     * 质检明细数据库访问对象。
     */
    @Autowired
    private QcInspectionItemMapper qcInspectionItemMapper;

    /**
     * 按质检单ID查询质检明细。
     */
    @Override
    public List<QcInspectionItem> findByInspectionId(Long inspectionId) {
        return qcInspectionItemMapper.findByInspectionId(inspectionId);
    }

    /**
     * 按质检明细ID查询质检明细。
     */
    @Override
    public QcInspectionItem findById(Long id) {
        return qcInspectionItemMapper.findById(id);
    }

    /**
     * 新增质检明细。
     */
    @Override
    public QcInspectionItem create(QcInspectionItem qcInspectionItem) {
        normalizeItem(qcInspectionItem);
        qcInspectionItemMapper.insert(qcInspectionItem);
        return qcInspectionItemMapper.findById(qcInspectionItem.getId());
    }

    /**
     * 修改质检明细。
     */
    @Override
    public QcInspectionItem update(QcInspectionItem qcInspectionItem) {
        QcInspectionItem existing = qcInspectionItemMapper.findById(qcInspectionItem.getId());
        if (existing == null) {
            throw new IllegalArgumentException("质检明细不存在");
        }
        mergeMissingFields(qcInspectionItem, existing);
        normalizeItem(qcInspectionItem);
        qcInspectionItemMapper.update(qcInspectionItem);
        return qcInspectionItemMapper.findById(qcInspectionItem.getId());
    }

    /**
     * 删除指定质检单下的全部明细。
     */
    @Override
    public void deleteByInspectionId(Long inspectionId) {
        qcInspectionItemMapper.deleteByInspectionId(inspectionId);
    }

    /**
     * 删除单条质检明细。
     */
    @Override
    public void deleteById(Long id) {
        qcInspectionItemMapper.deleteById(id);
    }

    /**
     * 标准化并校验质检明细数量和结果。
     */
    private void normalizeItem(QcInspectionItem item) {
        if (item == null) {
            throw new IllegalArgumentException("质检明细不能为空");
        }
        if (item.getInspectionId() == null) {
            throw new IllegalArgumentException("质检单ID不能为空");
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
     * 修改明细时补齐未传字段，避免旧数据被空值覆盖。
     */
    private void mergeMissingFields(QcInspectionItem target, QcInspectionItem existing) {
        if (target.getInspectionId() == null) {
            target.setInspectionId(existing.getInspectionId());
        }
        if (target.getMaterialId() == null) {
            target.setMaterialId(existing.getMaterialId());
        }
        if (target.getLotId() == null) {
            target.setLotId(existing.getLotId());
        }
        if (target.getInspectionItem() == null || target.getInspectionItem().isBlank()) {
            target.setInspectionItem(existing.getInspectionItem());
        }
        if (target.getInspectionStandard() == null) {
            target.setInspectionStandard(existing.getInspectionStandard());
        }
        if (target.getActualValue() == null) {
            target.setActualValue(existing.getActualValue());
        }
        if (target.getSampleQuantity() == null) {
            target.setSampleQuantity(existing.getSampleQuantity());
        }
        if (target.getQualifiedQuantity() == null) {
            target.setQualifiedQuantity(existing.getQualifiedQuantity());
        }
        if (target.getDefectType() == null) {
            target.setDefectType(existing.getDefectType());
        }
        if (target.getDefectReason() == null) {
            target.setDefectReason(existing.getDefectReason());
        }
        if (target.getHandleMethod() == null) {
            target.setHandleMethod(existing.getHandleMethod());
        }
        if (target.getItemResult() == null || target.getItemResult().isBlank()) {
            target.setItemResult(existing.getItemResult());
        }
        if (target.getRemark() == null) {
            target.setRemark(existing.getRemark());
        }
    }

    /**
     * 将空数量转换为0。
     */
    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
