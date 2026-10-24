package com.mouse.mes.service;

import com.mouse.mes.entity.QcInspection;

import java.util.List;

public interface QcInspectionService {

    /**
     * 查询全部未删除质检单。
     */
    List<QcInspection> findAll();

    /**
     * 按质检单ID查询质检单。
     */
    QcInspection findById(Long id);

    /**
     * 按质检单号查询质检单。
     */
    QcInspection findByInspectionNo(String inspectionNo);

    /**
     * 按质检类型查询质检单。
     */
    List<QcInspection> findByInspectionType(String inspectionType);

    /**
     * 按质检结果查询质检单。
     */
    List<QcInspection> findByResult(String result);

    /**
     * 新增质检单和质检明细。
     */
    QcInspection create(QcInspection qcInspection);

    /**
     * 修改质检单和质检明细。
     */
    QcInspection update(QcInspection qcInspection);

    /**
     * 逻辑删除质检单。
     */
    void delete(Long id);
}
