package com.mouse.mes.mapper;

import com.mouse.mes.entity.StockIn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockInMapper {
    List<StockIn> findAll();
    StockIn findById(@Param("id") Long id);
    StockIn findByStockInNo(@Param("stockInNo") String stockInNo);
    int insert(StockIn stockIn);
    int update(StockIn stockIn);
    int delete(@Param("id") Long id);
}
