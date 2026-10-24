package com.mouse.mes.mapper;

import com.mouse.mes.entity.Maintenance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MaintenanceMapper {
    /**
     * 查询全部维修单。
     */
    List<Maintenance> findAll();

    /**
     * 按维修单ID查询维修单。
     */
    Maintenance findById(@Param("id") Long id);

    /**
     * 按维修单号查询维修单。
     */
    Maintenance findByMaintenanceNo(@Param("maintenanceNo") String maintenanceNo);

    /**
     * 按设备ID查询维修单。
     */
    List<Maintenance> findByEquipmentId(@Param("equipmentId") Long equipmentId);

    /**
     * 查询设备未完成维修单。
     */
    Maintenance findUnfinishedByEquipmentId(@Param("equipmentId") Long equipmentId);

    /**
     * 新增维修单。
     */
    int insert(Maintenance maintenance);

    /**
     * 修改维修单。
     */
    int update(Maintenance maintenance);

    /**
     * 删除维修单。
     */
    int delete(@Param("id") Long id);
}
