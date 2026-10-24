package com.mouse.mes.mapper;

import com.mouse.mes.entity.Bom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BomMapper {
    List<Bom> findAll();
    Bom findById(@Param("id") Long id);
    List<Bom> findByProductId(@Param("productId") Long productId);
    int insert(Bom bom);
    int update(Bom bom);
    int delete(@Param("id") Long id);
}