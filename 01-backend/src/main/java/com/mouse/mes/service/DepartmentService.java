package com.mouse.mes.service;

import com.mouse.mes.entity.Department;

import java.util.List;

public interface DepartmentService {
    List<Department> findAll();
    Department findById(Long id);
    List<Department> findByParentId(Long parentId);
    Department create(Department department);
    Department update(Department department);
    void delete(Long id);
}