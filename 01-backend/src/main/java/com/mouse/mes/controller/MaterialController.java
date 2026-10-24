package com.mouse.mes.controller;

import com.mouse.mes.entity.Material;
import com.mouse.mes.service.MaterialService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @GetMapping
    public JsonResult<List<Material>> getAllMaterials() {
        return JsonResult.success(materialService.findAll());
    }

    @GetMapping("/list")
    public JsonResult<List<Material>> listMaterials() {
        return JsonResult.success(materialService.findAll());
    }

    @GetMapping("/non-product")
    public JsonResult<List<Material>> getNonProductMaterials() {
        return JsonResult.success(materialService.findByTypes(List.of(1, 2)));
    }

    @GetMapping("/{id}")
    public JsonResult<Material> getMaterialById(@PathVariable Long id) {
        Material material = materialService.findById(id);
        if (material == null) {
            return JsonResult.error("物料不存在");
        }
        return JsonResult.success(material);
    }

    @GetMapping("/materialcode/{materialCode}")
    public JsonResult<Material> getMaterialByMaterialCode(@PathVariable String materialCode) {
        Material material = materialService.findByMaterialCode(materialCode);
        if (material == null) {
            return JsonResult.error("物料不存在");
        }
        return JsonResult.success(material);
    }

    @PostMapping
    public JsonResult<Material> createMaterial(@RequestBody Material material) {
        try {
            return JsonResult.success(materialService.create(material));
        } catch (IllegalArgumentException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public JsonResult<Material> updateMaterial(@PathVariable Long id, @RequestBody Material material) {
        try {
            material.setId(id);
            return JsonResult.success(materialService.update(material));
        } catch (IllegalArgumentException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public JsonResult<Void> deleteMaterial(@PathVariable Long id) {
        try {
            materialService.delete(id);
            return JsonResult.success();
        } catch (IllegalArgumentException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/stock/{quantity}")
    public JsonResult<Void> updateStock(@PathVariable Long id, @PathVariable Integer quantity) {
        try {
            materialService.updateStock(id, quantity);
            return JsonResult.success();
        } catch (IllegalArgumentException e) {
            return JsonResult.error(e.getMessage());
        }
    }
}