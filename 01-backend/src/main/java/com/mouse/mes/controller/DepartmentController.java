package com.mouse.mes.controller;

import com.mouse.mes.entity.Department;
import com.mouse.mes.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @GetMapping
    public List<Department> getAllDepartments() {
        return departmentService.findAll();
    }

    @GetMapping("/{id}")
    public Department getDepartmentById(@PathVariable Long id) {
        return departmentService.findById(id);
    }

    @GetMapping("/parent/{parentId}")
    public List<Department> getDepartmentsByParentId(@PathVariable Long parentId) {
        return departmentService.findByParentId(parentId);
    }

    @PostMapping
    public Department createDepartment(@RequestBody Department department) {
        return departmentService.create(department);
    }

    @PutMapping("/{id}")
    public Department updateDepartment(@PathVariable Long id, @RequestBody Department department) {
        department.setId(id);
        return departmentService.update(department);
    }

    @DeleteMapping("/{id}")
    public void deleteDepartment(@PathVariable Long id) {
        departmentService.delete(id);
    }
}
