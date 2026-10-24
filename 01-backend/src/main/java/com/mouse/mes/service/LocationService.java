package com.mouse.mes.service;

import com.mouse.mes.entity.Location;

import java.util.List;

public interface LocationService {

    /**
     * 查询全部未删除库位。
     */
    List<Location> findAll();

    /**
     * 按库位ID查询库位。
     */
    Location findById(Long id);

    /**
     * 查询指定仓库下的库位。
     */
    List<Location> findByWarehouseId(Long warehouseId);

    /**
     * 新增库位。
     */
    Location create(Location location);

    /**
     * 修改库位。
     */
    Location update(Location location);

    /**
     * 删除库位。
     */
    void delete(Long id);
}
