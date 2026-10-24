package com.mouse.mes.controller;

import com.mouse.mes.entity.Uom;
import com.mouse.mes.mapper.UomMapper;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/uom")
public class UomController {

    @Autowired
    private UomMapper uomMapper;

    @GetMapping("/list")
    public JsonResult<List<Uom>> findAll() {
        return JsonResult.success(uomMapper.findAll());
    }

    @GetMapping("/{id}")
    public JsonResult<Uom> findById(@PathVariable Long id) {
        Uom uom = uomMapper.findById(id);
        if (uom == null) {
            return JsonResult.error("计量单位不存在");
        }
        return JsonResult.success(uom);
    }

    @GetMapping("/code/{uomCode}")
    public JsonResult<Uom> findByUomCode(@PathVariable String uomCode) {
        Uom uom = uomMapper.findByUomCode(uomCode);
        if (uom == null) {
            return JsonResult.error("计量单位不存在");
        }
        return JsonResult.success(uom);
    }

    @PostMapping("/add")
    public JsonResult<Uom> create(@RequestBody Uom uom) {
        if (uom.getUomCode() == null || uom.getUomCode().isEmpty()) {
            return JsonResult.error("单位编码不能为空");
        }
        if (uom.getUomName() == null || uom.getUomName().isEmpty()) {
            return JsonResult.error("单位名称不能为空");
        }
        uom.setCreatedAt(new Date());
        uom.setUpdatedAt(new Date());
        uom.setIsDeleted(0);
        if (uom.getStatus() == null) {
            uom.setStatus("ACTIVE");
        }
        uomMapper.insert(uom);
        return JsonResult.success(uom);
    }

    @PutMapping("/{id}")
    public JsonResult<Uom> update(@PathVariable Long id, @RequestBody Uom uom) {
        Uom existing = uomMapper.findById(id);
        if (existing == null) {
            return JsonResult.error("计量单位不存在");
        }
        uom.setId(id);
        uom.setUpdatedAt(new Date());
        uomMapper.update(uom);
        return JsonResult.success(uomMapper.findById(id));
    }

    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Long id) {
        Uom existing = uomMapper.findById(id);
        if (existing == null) {
            return JsonResult.error("计量单位不存在");
        }
        existing.setIsDeleted(1);
        existing.setUpdatedAt(new Date());
        uomMapper.update(existing);
        return JsonResult.success();
    }
}
