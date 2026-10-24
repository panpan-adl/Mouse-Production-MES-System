package com.mouse.mes.mapper;

import com.mouse.mes.entity.StockOutItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockOutItemMapper {
    List<StockOutItem> findByStockOutId(@Param("stockOutId") Long stockOutId);
    int insert(StockOutItem stockOutItem);
    int deleteByStockOutId(@Param("stockOutId") Long stockOutId);
}
