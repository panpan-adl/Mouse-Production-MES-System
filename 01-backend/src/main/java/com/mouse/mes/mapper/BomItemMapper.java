package com.mouse.mes.mapper;

import com.mouse.mes.entity.BomItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BomItemMapper {
    List<BomItem> findByBomId(@Param("bomId") Long bomId);
    int insert(BomItem bomItem);
    int deleteByBomId(@Param("bomId") Long bomId);
}
