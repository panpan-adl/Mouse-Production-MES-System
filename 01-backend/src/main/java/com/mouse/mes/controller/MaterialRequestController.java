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

    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;
    @GetMapping("/task-materials/{id}") public Object taskMaterials(@PathVariable long id){
        var rows=db.queryForList("SELECT DISTINCT m.material_id FROM mm_task_context c JOIN mm_work_material m ON m.work_order_id=c.work_order_id WHERE c.task_id=?",id);
        if(rows.isEmpty()&&db.queryForObject("SELECT COUNT(*) FROM mm_task_context WHERE task_id=?",Long.class,id)==0)rows=db.queryForList("SELECT id material_id FROM t_material WHERE status=1 AND is_deleted=0");
        return JsonResult.success(rows);
    }
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
    public JsonResult<MaterialRequest> create(@RequestBody MaterialRequest materialRequest, jakarta.servlet.http.HttpServletRequest request) {
        try {
            var identity=new com.fasterxml.jackson.databind.ObjectMapper().convertValue(request.getAttribute("userInfo"),com.mouse.mes.dto.LoginResponse.class);
            materialRequest.setCreatedBy(identity.getId());
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