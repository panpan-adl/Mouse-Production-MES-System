package com.mouse.mes.controller;

import com.mouse.mes.entity.MaterialRequest;
import com.mouse.mes.service.MaterialRequestService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/material-requests")
public class MaterialRequestController {

    @Autowired
    private MaterialRequestService materialRequestService;

    @GetMapping
    public JsonResult<List<MaterialRequest>> findAll() {
        return JsonResult.success(materialRequestService.findAll());
    }

    @GetMapping("/{id}")
    public JsonResult<MaterialRequest> findById(@PathVariable Long id) {
        MaterialRequest materialRequest = materialRequestService.findById(id);
        if (materialRequest == null) {
            return JsonResult.error("物料请求不存在");
        }
        return JsonResult.success(materialRequest);
    }

    @GetMapping("/request-no/{requestNo}")
    public JsonResult<MaterialRequest> findByRequestNo(@PathVariable String requestNo) {
        MaterialRequest materialRequest = materialRequestService.findByRequestNo(requestNo);
        if (materialRequest == null) {
            return JsonResult.error("物料请求不存在");
        }
        return JsonResult.success(materialRequest);
    }

    @GetMapping("/status/{requestStatus}")
    public JsonResult<List<MaterialRequest>> findByStatus(@PathVariable String requestStatus) {
        return JsonResult.success(materialRequestService.findByStatus(requestStatus));
    }

    @PostMapping
    public JsonResult<MaterialRequest> create(@RequestBody MaterialRequest materialRequest) {
        try {
            return JsonResult.success(materialRequestService.create(materialRequest));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public JsonResult<MaterialRequest> update(@PathVariable Long id, @RequestBody MaterialRequest materialRequest) {
        try {
            materialRequest.setId(id);
            return JsonResult.success(materialRequestService.update(materialRequest));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/request-purchase")
    public JsonResult<MaterialRequest> requestPurchase(@PathVariable Long id) {
        try {
            return JsonResult.success(materialRequestService.requestPurchase(id));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/purchase-ordered")
    public JsonResult<MaterialRequest> markPurchaseOrdered(@PathVariable Long id) {
        try {
            return JsonResult.success(materialRequestService.markPurchaseOrdered(id));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Long id) {
        MaterialRequest existing = materialRequestService.findById(id);
        if (existing == null) {
            return JsonResult.error("物料请求不存在");
        }
        materialRequestService.delete(id);
        return JsonResult.success();
    }
}