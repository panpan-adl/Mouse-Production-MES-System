package com.mouse.mes.mapper;

import com.mouse.mes.entity.Dict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DictMapper {
    List<Dict> findAll();
    Dict findById(@Param("id") Long id);
    Dict findByDictType(@Param("dictType") String dictType);
    int insert(Dict dict);
    int update(Dict dict);
    int delete(@Param("id") Long id);
}