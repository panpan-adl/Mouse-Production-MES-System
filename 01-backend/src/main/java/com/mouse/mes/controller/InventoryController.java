package com.mouse.mes.controller;

import com.mouse.mes.entity.Inventory;
import com.mouse.mes.entity.InventoryTransaction;
import com.mouse.mes.service.InventoryService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    /**
     * 库存业务服务，负责库存查询、低库存预警、库存流水和手工维护。
     */
    @Autowired
    private InventoryService inventoryService;

    /**
     * 查询全部库存。
     */
    @GetMapping("/list")
    public JsonResult<List<Inventory>> findAll() {
        return JsonResult.success(inventoryService.findAll());
    }

    /**
     * 按库存ID查询库存。
     */
    @GetMapping("/{id}")
    public JsonResult<Inventory> findById(@PathVariable Long id) {
        Inventory inventory = inventoryService.findById(id);
        if (inventory == null) {
            return JsonResult.error("库存记录不存在");
        }
        return JsonResult.success(inventory);
    }

    /**
     * 按仓库ID查询库存。
     */
    @GetMapping("/warehouse/{warehouseId}")
    public JsonResult<List<Inventory>> findByWarehouseId(@PathVariable Long warehouseId) {
        return JsonResult.success(inventoryService.findByWarehouseId(warehouseId));
    }

    /**
     * 按物料ID查询库存。
     */
    @GetMapping("/material/{materialId}")
    public JsonResult<List<Inventory>> findByMaterialId(@PathVariable Long materialId) {
        return JsonResult.success(inventoryService.findByMaterialId(materialId));
    }

    /**
     * 按物料、仓库、库位组合查询库存。
     */
    @GetMapping("/search")
    public JsonResult<List<Inventory>> search(@RequestParam(required = false) Long materialId,
                                              @RequestParam(required = false) Long warehouseId,
                                              @RequestParam(required = false) Long locationId) {
        return JsonResult.success(inventoryService.search(materialId, warehouseId, locationId));
    }

    /**
     * 查询低库存记录。
     */
    @GetMapping("/low-stock")
    public JsonResult<List<Inventory>> findLowStock() {
        return JsonResult.success(inventoryService.findLowStock());
    }

    /**
     * 查询库存流水。
     */
    @GetMapping("/transactions")
    public JsonResult<List<InventoryTransaction>> findTransactions() {
        return JsonResult.success(inventoryService.findTransactions());
    }

    /**
     * 手工新增库存记录。
     */
    @PostMapping("/add")
    public JsonResult<Inventory> create(@RequestBody Inventory inventory) {
        if (inventory.getWarehouseId() == null) {
            return JsonResult.error("仓库ID不能为空");
        }
        if (inventory.getMaterialId() == null) {
            return JsonResult.error("物料ID不能为空");
        }
        return JsonResult.error("库存须通过入出库单据变动，请进入生产与仓储闭环");
    }

    /**
     * 手工修改库存记录。
     */
    @PutMapping("/{id}")
    public JsonResult<Inventory> update(@PathVariable Long id, @RequestBody Inventory inventory) {
        Inventory existing = inventoryService.findById(id);
        if (existing == null) {
            return JsonResult.error("库存记录不存在");
        }
        inventory.setId(id);
        return JsonResult.error("库存余额不能直接改写，请保留单据与流水一致");
    }

    /**
     * 逻辑删除库存记录。
     */
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Long id) {
        Inventory existing = inventoryService.findById(id);
        if (existing == null) {
            return JsonResult.error("库存记录不存在");
        }
        return JsonResult.error("库存记录不能删除，请通过有来源的业务单据变动");
    }
}
