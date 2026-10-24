package com.mouse.mes.controller;

import com.mouse.mes.entity.QcInspection;
import com.mouse.mes.service.QcInspectionService;
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
@RequestMapping("/api/qc-inspection")
public class QcInspectionController {

    /**
     * 质检业务服务，负责质检单、质检明细、合格率和业务状态流转。
     */
    @Autowired
    private QcInspectionService qcInspectionService;

    /**
     * 查询全部质检单。
     */
    @GetMapping("/list")
    public JsonResult<List<QcInspection>> findAll() {
        return JsonResult.success(qcInspectionService.findAll());
    }

    /**
     * 按质检单ID查询质检单。
     */
    @GetMapping("/{id}")
    public JsonResult<QcInspection> findById(@PathVariable Long id) {
        QcInspection qcInspection = qcInspectionService.findById(id);
        if (qcInspection == null) {
            return JsonResult.error("质检单不存在");
        }
        return JsonResult.success(qcInspection);
    }

    /**
     * 按质检单号查询质检单。
     */
    @GetMapping("/no/{inspectionNo}")
    public JsonResult<QcInspection> findByInspectionNo(@PathVariable String inspectionNo) {
        QcInspection qcInspection = qcInspectionService.findByInspectionNo(inspectionNo);
        if (qcInspection == null) {
            return JsonResult.error("质检单不存在");
        }
        return JsonResult.success(qcInspection);
    }

    /**
     * 按质检类型查询质检单。
     */
    @GetMapping("/type/{inspectionType}")
    public JsonResult<List<QcInspection>> findByInspectionType(@PathVariable String inspectionType) {
        return JsonResult.success(qcInspectionService.findByInspectionType(inspectionType));
    }

    /**
     * 按质检结果查询质检单。
     */
    @GetMapping("/result/{result}")
    public JsonResult<List<QcInspection>> findByResult(@PathVariable String result) {
        return JsonResult.success(qcInspectionService.findByResult(result));
    }

    /**
     * 新增质检单，保存主单和多条检测项明细。
     */
    @PostMapping("/add")
    public JsonResult<QcInspection> create(@RequestBody QcInspection qcInspection) {
        try {
            return JsonResult.success(qcInspectionService.create(qcInspection));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 修改质检单，重新计算合格率和业务放行状态。
     */
    @PutMapping("/{id}")
    public JsonResult<QcInspection> update(@PathVariable Long id, @RequestBody QcInspection qcInspection) {
        try {
            qcInspection.setId(id);
            return JsonResult.success(qcInspectionService.update(qcInspection));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 删除质检单；不存在时返回错误。
     */
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Long id) {
        try {
            qcInspectionService.delete(id);
            return JsonResult.success();
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }
}
