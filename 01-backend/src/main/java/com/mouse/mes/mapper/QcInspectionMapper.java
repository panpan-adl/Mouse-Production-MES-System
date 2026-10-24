package com.mouse.mes.mapper;

import com.mouse.mes.entity.QcInspection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QcInspectionMapper {
    /**
     * 查询全部未删除质检单。
     */
    List<QcInspection> findAll();

    /**
     * 按质检单ID查询质检单。
     */
    QcInspection findById(@Param("id") Long id);

    /**
     * 按质检单号查询质检单。
     */
    QcInspection findByInspectionNo(@Param("inspectionNo") String inspectionNo);

    /**
     * 按质检类型查询质检单。
     */
    List<QcInspection> findByInspectionType(@Param("inspectionType") String inspectionType);

    /**
     * 按质检结果查询质检单。
     */
    List<QcInspection> findByResult(@Param("result") String result);

    /**
     * 新增质检单。
     */
    int insert(QcInspection qcInspection);

    /**
     * 修改质检单。
     */
    int update(QcInspection qcInspection);

    /**
     * 逻辑删除质检单。
     */
    int delete(@Param("id") Long id);
}
