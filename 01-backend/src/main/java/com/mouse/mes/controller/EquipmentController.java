package com.mouse.mes.controller;

import com.mouse.mes.entity.Equipment;
import com.mouse.mes.service.EquipmentService;
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
@RequestMapping("/api/equipments")
public class EquipmentController {

    /**
     * 设备业务服务，用于设备台账新增、修改、删除和查询。
     */
    @Autowired
    private EquipmentService equipmentService;

    /**
     * 查询全部设备。
     */
    @GetMapping
    public JsonResult<List<Equipment>> getAllEquipments() {
        return JsonResult.success(equipmentService.findAll());
    }

    /**
     * 查询全部设备，兼容/list路径。
     */
    @GetMapping("/list")
    public JsonResult<List<Equipment>> listEquipments() {
        return JsonResult.success(equipmentService.findAll());
    }

    /**
     * 按设备ID查询设备。
     */
    @GetMapping("/{id}")
    public JsonResult<Equipment> getEquipmentById(@PathVariable Long id) {
        Equipment equipment = equipmentService.findById(id);
        if (equipment == null) {
            return JsonResult.error("设备不存在");
        }
        return JsonResult.success(equipment);
    }

    /**
     * 按设备编号查询设备。
     */
    @GetMapping("/equipmentcode/{equipmentCode}")
    public JsonResult<Equipment> getEquipmentByEquipmentCode(@PathVariable String equipmentCode) {
        Equipment equipment = equipmentService.findByEquipmentCode(equipmentCode);
        if (equipment == null) {
            return JsonResult.error("设备不存在");
        }
        return JsonResult.success(equipment);
    }

    /**
     * 新增设备。
     */
    @PostMapping
    public JsonResult<Equipment> createEquipment(@RequestBody Equipment equipment) {
        try {
            return JsonResult.success(equipmentService.create(equipment));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 修改设备。
     */
    @PutMapping("/{id}")
    public JsonResult<Equipment> updateEquipment(@PathVariable Long id, @RequestBody Equipment equipment) {
        try {
            equipment.setId(id);
            return JsonResult.success(equipmentService.update(equipment));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 删除设备；不存在时返回错误。
     */
    @DeleteMapping("/{id}")
    public JsonResult<Void> deleteEquipment(@PathVariable Long id) {
        try {
            equipmentService.delete(id);
            return JsonResult.success();
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }
}
