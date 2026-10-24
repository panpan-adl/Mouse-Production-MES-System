package com.mouse.mes.mapper;

import com.mouse.mes.entity.Warehouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WarehouseMapper {
    List<Warehouse> findAll();
    Warehouse findById(@Param("id") Long id);
    Warehouse findByWarehouseCode(@Param("warehouseCode") String warehouseCode);
    int insert(Warehouse warehouse);
    int update(Warehouse warehouse);
    int delete(@Param("id") Long id);
}