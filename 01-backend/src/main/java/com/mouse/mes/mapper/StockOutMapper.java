package com.mouse.mes.mapper;

import com.mouse.mes.entity.StockOut;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockOutMapper {
    List<StockOut> findAll();
    StockOut findById(@Param("id") Long id);
    StockOut findByStockOutNo(@Param("stockOutNo") String stockOutNo);
    int insert(StockOut stockOut);
    int update(StockOut stockOut);
    int delete(@Param("id") Long id);
}
