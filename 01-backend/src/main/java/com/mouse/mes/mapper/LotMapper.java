package com.mouse.mes.mapper;

import com.mouse.mes.entity.Lot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LotMapper {
    List<Lot> findAll();
    Lot findById(@Param("id") Long id);
    List<Lot> findByMaterialId(@Param("materialId") Long materialId);
    int insert(Lot lot);
    int update(Lot lot);
    int delete(@Param("id") Long id);
}
