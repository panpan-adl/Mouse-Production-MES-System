package com.mouse.mes.mapper;

import com.mouse.mes.entity.InventoryOut;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InventoryOutMapper {
    List<InventoryOut> findAll();
    InventoryOut findById(@Param("id") Long id);
    InventoryOut findByOutNo(@Param("outNo") String outNo);
    int insert(InventoryOut inventoryOut);
    int update(InventoryOut inventoryOut);
}