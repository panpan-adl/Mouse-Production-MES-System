package com.mouse.mes.controller;

import com.mouse.mes.entity.QualityCheck;
import com.mouse.mes.service.QualityCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quality-checks")
public class QualityCheckController {

    @Autowired
    private QualityCheckService qualityCheckService;

    @GetMapping
    public List<QualityCheck> getAllQualityChecks() {
        return qualityCheckService.findAll();
    }

    @GetMapping("/{id}")
    public QualityCheck getQualityCheckById(@PathVariable Long id) {
        return qualityCheckService.findById(id);
    }

    @GetMapping("/product/{productId}")
    public List<QualityCheck> getQualityChecksByProductId(@PathVariable Long productId) {
        return qualityCheckService.findByProductId(productId);
    }

    @GetMapping("/task/{taskId}")
    public List<QualityCheck> getQualityChecksByTaskId(@PathVariable Long taskId) {
        return qualityCheckService.findByTaskId(taskId);
    }

    @PostMapping
    public QualityCheck createQualityCheck(@RequestBody QualityCheck qualityCheck) {
        return qualityCheckService.create(qualityCheck);
    }

    @PutMapping("/{id}")
    public QualityCheck updateQualityCheck(@PathVariable Long id, @RequestBody QualityCheck qualityCheck) {
        qualityCheck.setId(id);
        return qualityCheckService.update(qualityCheck);
    }
}