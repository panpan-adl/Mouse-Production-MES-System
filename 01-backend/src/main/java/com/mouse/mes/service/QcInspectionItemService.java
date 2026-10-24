package com.mouse.mes.service;

import com.mouse.mes.entity.QcInspectionItem;

import java.util.List;

public interface QcInspectionItemService {

    /**
     * 按质检单ID查询质检明细。
     */
    List<QcInspectionItem> findByInspectionId(Long inspectionId);

    /**
     * 按质检明细ID查询质检明细。
     */
    QcInspectionItem findById(Long id);

    /**
     * 新增质检明细。
     */
    QcInspectionItem create(QcInspectionItem qcInspectionItem);

    /**
     * 修改质检明细。
     */
    QcInspectionItem update(QcInspectionItem qcInspectionItem);

    /**
     * 删除指定质检单下的全部明细。
     */
    void deleteByInspectionId(Long inspectionId);

    /**
     * 删除单条质检明细。
     */
    void deleteById(Long id);
}
