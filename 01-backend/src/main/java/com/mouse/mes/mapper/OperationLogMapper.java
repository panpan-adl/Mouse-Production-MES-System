package com.mouse.mes.mapper;

import com.mouse.mes.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OperationLogMapper {
    List<OperationLog> findAll();
    OperationLog findById(@Param("id") Long id);
    List<OperationLog> findByUserId(@Param("userId") Long userId);
    int insert(OperationLog operationLog);
}