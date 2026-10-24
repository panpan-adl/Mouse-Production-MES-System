package com.mouse.mes.mapper;

import com.mouse.mes.entity.StockInItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockInItemMapper {
    List<StockInItem> findByStockInId(@Param("stockInId") Long stockInId);
    int insert(StockInItem stockInItem);
    int deleteByStockInId(@Param("stockInId") Long stockInId);
}
