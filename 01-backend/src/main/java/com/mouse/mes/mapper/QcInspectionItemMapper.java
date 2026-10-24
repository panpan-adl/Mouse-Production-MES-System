package com.mouse.mes.mapper;

import com.mouse.mes.entity.QcInspectionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QcInspectionItemMapper {
    /**
     * 按质检单ID查询明细。
     */
    List<QcInspectionItem> findByInspectionId(@Param("inspectionId") Long inspectionId);

    /**
     * 按质检明细ID查询明细。
     */
    QcInspectionItem findById(@Param("id") Long id);

    /**
     * 新增质检明细。
     */
    int insert(QcInspectionItem qcInspectionItem);

    /**
     * 修改质检明细。
     */
    int update(QcInspectionItem qcInspectionItem);

    /**
     * 删除指定质检单下的全部明细。
     */
    int deleteByInspectionId(@Param("inspectionId") Long inspectionId);

    /**
     * 按明细ID删除质检明细。
     */
    int deleteById(@Param("id") Long id);
}
