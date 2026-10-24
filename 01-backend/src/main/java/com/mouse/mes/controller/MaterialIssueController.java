package com.mouse.mes.controller;

import com.mouse.mes.entity.MaterialIssue;
import com.mouse.mes.entity.MaterialIssueItem;
import com.mouse.mes.service.MaterialIssueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/material-issues")
public class MaterialIssueController {

    @Autowired
    private MaterialIssueService materialIssueService;

    @GetMapping
    public List<MaterialIssue> getAllMaterialIssues() {
        return materialIssueService.findAll();
    }

    @GetMapping("/{id}")
    public MaterialIssue getMaterialIssueById(@PathVariable Long id) {
        return materialIssueService.findById(id);
    }

    @GetMapping("/issue-no/{issueNo}")
    public MaterialIssue getMaterialIssueByIssueNo(@PathVariable String issueNo) {
        return materialIssueService.findByIssueNo(issueNo);
    }

    @GetMapping("/{id}/items")
    public List<MaterialIssueItem> getMaterialIssueItems(@PathVariable Long id) {
        return materialIssueService.findItemsByIssueId(id);
    }

    @PostMapping
    public MaterialIssue createMaterialIssue(@RequestBody MaterialIssue materialIssue) {
        return materialIssueService.create(materialIssue);
    }

    @PutMapping("/{id}")
    public MaterialIssue updateMaterialIssue(@PathVariable Long id, @RequestBody MaterialIssue materialIssue) {
        materialIssue.setId(id);
        return materialIssueService.update(materialIssue);
    }

    @DeleteMapping("/{id}")
    public void deleteMaterialIssue(@PathVariable Long id) {
        materialIssueService.delete(id);
    }
}