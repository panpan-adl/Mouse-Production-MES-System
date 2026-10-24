package com.mouse.mes.controller;

import com.mouse.mes.entity.Supplier;
import com.mouse.mes.service.SupplierService;
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
@RequestMapping("/api/suppliers")
public class SupplierController {

    /**
     * 供应商业务服务，负责供应商新增、修改、删除、查询和禁用。
     */
    @Autowired
    private SupplierService supplierService;

    /**
     * 查询全部未删除供应商，活跃供应商排在禁用供应商前面。
     */
    @GetMapping
    public JsonResult<List<Supplier>> getAllSuppliers() {
        return JsonResult.success(supplierService.findAll());
    }

    /**
     * 查询全部未删除供应商，兼容 /list 路径。
     */
    @GetMapping("/list")
    public JsonResult<List<Supplier>> listSuppliers() {
        return JsonResult.success(supplierService.findAll());
    }

    /**
     * 按供应商编码、名称和状态组合查询供应商；status为空时查询全部未删除供应商。
     */
    @GetMapping("/search")
    public JsonResult<List<Supplier>> searchSuppliers(@RequestParam(required = false) String supplierCode,
                                                      @RequestParam(required = false) String supplierName,
                                                      @RequestParam(required = false) Integer status) {
        try {
            return JsonResult.success(supplierService.search(supplierCode, supplierName, status));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 按供应商ID查询未删除供应商。
     */
    @GetMapping("/{id}")
    public JsonResult<Supplier> getSupplierById(@PathVariable Long id) {
        Supplier supplier = supplierService.findById(id);
        if (supplier == null) {
            return JsonResult.error("供应商不存在");
        }
        return JsonResult.success(supplier);
    }

    /**
     * 按供应商编码查询未删除供应商。
     */
    @GetMapping("/suppliercode/{supplierCode}")
    public JsonResult<Supplier> getSupplierBySupplierCode(@PathVariable String supplierCode) {
        Supplier supplier = supplierService.findBySupplierCode(supplierCode);
        if (supplier == null) {
            return JsonResult.error("供应商不存在");
        }
        return JsonResult.success(supplier);
    }

    /**
     * 新增供应商。
     */
    @PostMapping
    public JsonResult<Supplier> createSupplier(@RequestBody Supplier supplier) {
        try {
            return JsonResult.success(supplierService.create(supplier));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 修改供应商。
     */
    @PutMapping("/{id}")
    public JsonResult<Supplier> updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        try {
            supplier.setId(id);
            return JsonResult.success(supplierService.update(supplier));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 禁用供应商，禁用后不能再创建采购订单，但仍可查询和恢复。
     */
    @PutMapping("/{id}/disable")
    public JsonResult<Supplier> disableSupplier(@PathVariable Long id) {
        try {
            return JsonResult.success(supplierService.disable(id));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 启用供应商，用于恢复被禁用的供应商。
     */
    @PutMapping("/{id}/enable")
    public JsonResult<Supplier> enableSupplier(@PathVariable Long id) {
        try {
            return JsonResult.success(supplierService.enable(id));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 删除供应商。
     */
    @DeleteMapping("/{id}")
    public JsonResult<Void> deleteSupplier(@PathVariable Long id) {
        Supplier existing = supplierService.findById(id);
        if (existing == null) {
            return JsonResult.error("供应商不存在");
        }
        supplierService.delete(id);
        return JsonResult.success();
    }
}
