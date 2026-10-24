package com.mouse.mes.mapper;

import com.mouse.mes.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DepartmentMapper {
    List<Department> findAll();
    Department findById(@Param("id") Long id);
    List<Department> findByParentId(@Param("parentId") Long parentId);
    int insert(Department department);
    int update(Department department);
    int delete(@Param("id") Long id);
}