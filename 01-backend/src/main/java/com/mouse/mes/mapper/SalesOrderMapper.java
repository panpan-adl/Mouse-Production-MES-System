package com.mouse.mes.mapper;

import com.mouse.mes.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SalesOrderMapper {
    List<SalesOrder> findAll();
    SalesOrder findById(@Param("id") Long id);
    SalesOrder findBySoNo(@Param("soNo") String soNo);
    int insert(SalesOrder salesOrder);
    int update(SalesOrder salesOrder);
    int delete(@Param("id") Long id);
}
