package com.mouse.mes.impl;

import com.mouse.mes.entity.Department;
import com.mouse.mes.mapper.DepartmentMapper;
import com.mouse.mes.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    public List<Department> findAll() {
        return departmentMapper.findAll();
    }

    @Override
    public Department findById(Long id) {
        return departmentMapper.findById(id);
    }

    @Override
    public List<Department> findByParentId(Long parentId) {
        return departmentMapper.findByParentId(parentId);
    }

    @Override
    public Department create(Department department) {
        department.setCreateTime(new Date());
        department.setUpdateTime(new Date());
        if (department.getStatus() == null) {
            department.setStatus(1);
        }
        if (department.getSort() == null) {
            department.setSort(0);
        }
        if (department.getParentId() == null) {
            department.setParentId(0L);
        }
        departmentMapper.insert(department);
        return department;
    }

    @Override
    public Department update(Department department) {
        department.setUpdateTime(new Date());
        departmentMapper.update(department);
        return departmentMapper.findById(department.getId());
    }

    @Override
    public void delete(Long id) {
        departmentMapper.delete(id);
    }
}