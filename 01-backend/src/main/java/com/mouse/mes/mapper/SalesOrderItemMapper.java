package com.mouse.mes.mapper;

import com.mouse.mes.entity.SalesOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SalesOrderItemMapper {
    List<SalesOrderItem> findBySoId(@Param("soId") Long soId);
    int insert(SalesOrderItem salesOrderItem);
    int deleteBySoId(@Param("soId") Long soId);
}
