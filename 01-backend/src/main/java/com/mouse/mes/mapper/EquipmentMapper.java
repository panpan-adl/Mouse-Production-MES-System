package com.mouse.mes.mapper;

import com.mouse.mes.entity.Equipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EquipmentMapper {
    /**
     * 查询全部未删除设备。
     */
    List<Equipment> findAll();

    /**
     * 按设备ID查询设备。
     */
    Equipment findById(@Param("id") Long id);

    /**
     * 按设备编号查询设备。
     */
    Equipment findByEquipmentCode(@Param("equipmentCode") String equipmentCode);

    /**
     * 新增设备。
     */
    int insert(Equipment equipment);

    /**
     * 修改设备。
     */
    int update(Equipment equipment);

    /**
     * 逻辑删除设备。
     */
    int delete(@Param("id") Long id);
}
