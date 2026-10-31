package com.mouse.mes.impl;

import com.mouse.mes.entity.Location;
import com.mouse.mes.mapper.LocationMapper;
import com.mouse.mes.mapper.WarehouseMapper;
import com.mouse.mes.service.LocationService;
import com.mouse.mes.service.LocationUsageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class LocationServiceImpl implements LocationService {

    /**
     * 库位数据库访问对象。
     */
    @Autowired
    private LocationMapper locationMapper;

    /**
     * 仓库数据库访问对象，用于校验库位所属仓库。
     */
    @Autowired
    private WarehouseMapper warehouseMapper;

    @Autowired
    private LocationUsageService locationUsage;

    /**
     * 查询全部未删除库位。
     */
    @Override
    public List<Location> findAll() {
        return locationMapper.findAll();
    }

    /**
     * 按库位ID查询库位。
     */
    @Override
    public Location findById(Long id) {
        return locationMapper.findById(id);
    }

    /**
     * 查询指定仓库下的库位。
     */
    @Override
    public List<Location> findByWarehouseId(Long warehouseId) {
        return locationMapper.findByWarehouseId(warehouseId);
    }

    /**
     * 新增库位。
     */
    @Override
    public Location create(Location location) {
        validateLocation(location);
        location.setCreatedAt(new Date());
        location.setUpdatedAt(new Date());
        location.setIsDeleted(0);
        if (location.getStatus() == null || location.getStatus().isBlank()) {
            location.setStatus("1");
        }
        locationMapper.insert(location);
        return locationMapper.findById(location.getId());
    }

    /**
     * 修改库位。
     */
    @Override
    @Transactional
    public Location update(Location location) {
        Location existing = locationMapper.findById(location.getId());
        if (existing == null) {
            throw new IllegalArgumentException("库位不存在");
        }
        mergeMissingFields(location, existing);
        validateLocation(location);
        location.setUpdatedAt(new Date());
        locationMapper.update(location);
        locationUsage.syncFoundationStatus(location.getId());
        return locationMapper.findById(location.getId());
    }

    /**
     * 删除库位。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        locationUsage.requireUnused(id);
        locationMapper.delete(id);
        locationUsage.removeFoundationMirror(id);
    }

    /**
     * 校验库位基础字段。
     */
    private void validateLocation(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("库位不能为空");
        }
        if (location.getWarehouseId() == null) {
            throw new IllegalArgumentException("仓库ID不能为空");
        }
        if (warehouseMapper.findById(location.getWarehouseId()) == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        if (location.getLocationCode() == null || location.getLocationCode().isBlank()) {
            throw new IllegalArgumentException("库位编码不能为空");
        }
        if (location.getLocationName() == null || location.getLocationName().isBlank()) {
            throw new IllegalArgumentException("库位名称不能为空");
        }
    }

    /**
     * 修改库位时补齐未传字段。
     */
    private void mergeMissingFields(Location target, Location existing) {
        if (target.getWarehouseId() == null) {
            target.setWarehouseId(existing.getWarehouseId());
        }
        if (target.getLocationCode() == null || target.getLocationCode().isBlank()) {
            target.setLocationCode(existing.getLocationCode());
        }
        if (target.getLocationName() == null || target.getLocationName().isBlank()) {
            target.setLocationName(existing.getLocationName());
        }
        if (target.getLocationType() == null) {
            target.setLocationType(existing.getLocationType());
        }
        if (target.getStatus() == null) {
            target.setStatus(existing.getStatus());
        }
        target.setCreatedAt(existing.getCreatedAt());
        target.setIsDeleted(existing.getIsDeleted());
    }
}
