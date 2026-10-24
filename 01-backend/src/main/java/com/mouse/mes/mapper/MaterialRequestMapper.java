package com.mouse.mes.mapper;

import com.mouse.mes.entity.MaterialRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MaterialRequestMapper {
    List<MaterialRequest> findAll();

    MaterialRequest findById(@Param("id") Long id);

    MaterialRequest findByRequestNo(@Param("requestNo") String requestNo);

    List<MaterialRequest> findByStatus(@Param("requestStatus") String requestStatus);

    int insert(MaterialRequest materialRequest);

    int update(MaterialRequest materialRequest);

    int delete(@Param("id") Long id);
}