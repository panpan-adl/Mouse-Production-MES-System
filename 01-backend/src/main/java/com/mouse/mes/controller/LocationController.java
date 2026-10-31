package com.mouse.mes.controller;

import com.mouse.mes.entity.Location;
import com.mouse.mes.service.LocationService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    /**
     * 库位业务服务，负责仓库下的库位管理。
     */
    @Autowired
    private LocationService locationService;

    /**
     * 查询全部库位。
     */
    @GetMapping("/list")
    public JsonResult<List<Location>> findAll() {
        return JsonResult.success(locationService.findAll());
    }

    /**
     * 按库位ID查询库位。
     */
    @GetMapping("/{id}")
    public JsonResult<Location> findById(@PathVariable Long id) {
        Location location = locationService.findById(id);
        if (location == null) {
            return JsonResult.error("库位不存在");
        }
        return JsonResult.success(location);
    }

    /**
     * 查询指定仓库下的库位。
     */
    @GetMapping("/warehouse/{warehouseId}")
    public JsonResult<List<Location>> findByWarehouseId(@PathVariable Long warehouseId) {
        return JsonResult.success(locationService.findByWarehouseId(warehouseId));
    }

    /**
     * 新增库位。
     */
    @PostMapping("/add")
    public JsonResult<Location> create(@RequestBody Location location) {
        try {
            return JsonResult.success(locationService.create(location));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 修改库位。
     */
    @PutMapping("/{id}")
    public JsonResult<Location> update(@PathVariable Long id, @RequestBody Location location) {
        try {
            location.setId(id);
            return JsonResult.success(locationService.update(location));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 删除库位。
     */
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Long id) {
        Location existing = locationService.findById(id);
        if (existing == null) {
            return JsonResult.error("库位不存在");
        }
        try {
            locationService.delete(id);
            return JsonResult.success();
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }
}
