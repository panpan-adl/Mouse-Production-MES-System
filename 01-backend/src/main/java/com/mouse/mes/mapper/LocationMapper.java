package com.mouse.mes.mapper;

import com.mouse.mes.entity.Location;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LocationMapper {
    List<Location> findAll();
    Location findById(@Param("id") Long id);
    List<Location> findByWarehouseId(@Param("warehouseId") Long warehouseId);
    int insert(Location location);
    int update(Location location);
    int delete(@Param("id") Long id);
}
