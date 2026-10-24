package com.mouse.mes.mapper;

import com.mouse.mes.entity.DictItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DictItemMapper {
    List<DictItem> findAll();
    DictItem findById(@Param("id") Long id);
    List<DictItem> findByDictType(@Param("dictType") String dictType);
    int insert(DictItem dictItem);
    int update(DictItem dictItem);
    int delete(@Param("id") Long id);
}