package com.mouse.mes.mapper;

import com.mouse.mes.entity.InventoryIn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InventoryInMapper {
    List<InventoryIn> findAll();
    InventoryIn findById(@Param("id") Long id);
    InventoryIn findByInNo(@Param("inNo") String inNo);
    int insert(InventoryIn inventoryIn);
    int update(InventoryIn inventoryIn);
}