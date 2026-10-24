package com.mouse.mes.service;

import com.mouse.mes.entity.Equipment;

import java.util.List;

public interface EquipmentService {
    /**
     * 查询全部未删除设备。
     */
    List<Equipment> findAll();

    /**
     * 按设备ID查询设备。
     */
    Equipment findById(Long id);

    /**
     * 按设备编号查询设备。
     */
    Equipment findByEquipmentCode(String equipmentCode);

    /**
     * 新增设备。
     */
    Equipment create(Equipment equipment);

    /**
     * 修改设备。
     */
    Equipment update(Equipment equipment);

    /**
     * 删除设备。
     */
    void delete(Long id);
}
