package com.mouse.mes.controller;

import com.mouse.mes.entity.Warehouse;
import com.mouse.mes.service.WarehouseService;
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
@RequestMapping("/api/warehouses")
public class WarehouseController {

    /**
     * 仓库业务服务，负责仓库新增、修改、删除和查询。
     */
    @Autowired
    private WarehouseService warehouseService;

    /**
     * 查询全部启用仓库。
     */
    @GetMapping
    public JsonResult<List<Warehouse>> getAllWarehouses() {
        return JsonResult.success(warehouseService.findAll());
    }

    /**
     * 查询全部启用仓库，兼容/list路径。
     */
    @GetMapping("/list")
    public JsonResult<List<Warehouse>> listWarehouses() {
        return JsonResult.success(warehouseService.findAll());
    }

    /**
     * 按仓库ID查询仓库。
     */
    @GetMapping("/{id}")
    public JsonResult<Warehouse> getWarehouseById(@PathVariable Long id) {
        Warehouse warehouse = warehouseService.findById(id);
        if (warehouse == null) {
            return JsonResult.error("仓库不存在");
        }
        return JsonResult.success(warehouse);
    }

    /**
     * 按仓库编码查询仓库。
     */
    @GetMapping("/warehousecode/{warehouseCode}")
    public JsonResult<Warehouse> getWarehouseByWarehouseCode(@PathVariable String warehouseCode) {
        Warehouse warehouse = warehouseService.findByWarehouseCode(warehouseCode);
        if (warehouse == null) {
            return JsonResult.error("仓库不存在");
        }
        return JsonResult.success(warehouse);
    }

    /**
     * 新增仓库。
     */
    @PostMapping
    public JsonResult<Warehouse> createWarehouse(@RequestBody Warehouse warehouse) {
        try {
            return JsonResult.success(warehouseService.create(warehouse));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 修改仓库。
     */
    @PutMapping("/{id}")
    public JsonResult<Warehouse> updateWarehouse(@PathVariable Long id, @RequestBody Warehouse warehouse) {
        try {
            warehouse.setId(id);
            return JsonResult.success(warehouseService.update(warehouse));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 删除仓库，删除后查询不到。
     */
    @DeleteMapping("/{id}")
    public JsonResult<Void> deleteWarehouse(@PathVariable Long id) {
        Warehouse existing = warehouseService.findById(id);
        if (existing == null) {
            return JsonResult.error("仓库不存在");
        }
        warehouseService.delete(id);
        return JsonResult.success();
    }
}
