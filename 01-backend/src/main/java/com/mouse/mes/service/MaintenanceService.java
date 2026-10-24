package com.mouse.mes.service;

import com.mouse.mes.entity.Maintenance;

import java.math.BigDecimal;
import java.util.List;

public interface MaintenanceService {
    /**
     * 查询全部维修单。
     */
    List<Maintenance> findAll();

    /**
     * 按维修单ID查询维修单。
     */
    Maintenance findById(Long id);

    /**
     * 按维修单号查询维修单。
     */
    Maintenance findByMaintenanceNo(String maintenanceNo);

    /**
     * 按设备ID查询维修单。
     */
    List<Maintenance> findByEquipmentId(Long equipmentId);

    /**
     * 创建设备维修单。
     */
    Maintenance create(Maintenance maintenance);

    /**
     * 修改维修单基础信息。
     */
    Maintenance update(Maintenance maintenance);

    /**
     * 指派维修人员。
     */
    Maintenance assign(Long id, Long userId, String userName);

    /**
     * 开始维修。
     */
    Maintenance start(Long id);

    /**
     * 完成维修。
     */
    Maintenance finish(Long id, String maintenanceContent, String result, BigDecimal cost, Boolean scrapEquipment);

    /**
     * 取消维修。
     */
    Maintenance cancel(Long id);

    /**
     * 删除维修单。
     */
    void delete(Long id);
}
