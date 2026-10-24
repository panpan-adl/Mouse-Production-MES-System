package com.mouse.mes.controller;

import com.mouse.mes.entity.QualityStandard;
import com.mouse.mes.service.QualityStandardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quality-standards")
public class QualityStandardController {

    @Autowired
    private QualityStandardService qualityStandardService;

    @GetMapping
    public List<QualityStandard> getAllQualityStandards() {
        return qualityStandardService.findAll();
    }

    @GetMapping("/{id}")
    public QualityStandard getQualityStandardById(@PathVariable Long id) {
        return qualityStandardService.findById(id);
    }

    @GetMapping("/product/{productId}")
    public List<QualityStandard> getQualityStandardsByProductId(@PathVariable Long productId) {
        return qualityStandardService.findByProductId(productId);
    }

    @PostMapping
    public QualityStandard createQualityStandard(@RequestBody QualityStandard qualityStandard) {
        return qualityStandardService.create(qualityStandard);
    }

    @PutMapping("/{id}")
    public QualityStandard updateQualityStandard(@PathVariable Long id, @RequestBody QualityStandard qualityStandard) {
        qualityStandard.setId(id);
        return qualityStandardService.update(qualityStandard);
    }

    @DeleteMapping("/{id}")
    public void deleteQualityStandard(@PathVariable Long id) {
        qualityStandardService.delete(id);
    }
}