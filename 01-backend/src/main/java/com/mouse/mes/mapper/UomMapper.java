package com.mouse.mes.mapper;

import com.mouse.mes.entity.Uom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UomMapper {
    List<Uom> findAll();
    Uom findById(@Param("id") Long id);
    Uom findByUomCode(@Param("uomCode") String uomCode);
    int insert(Uom uom);
    int update(Uom uom);
    int delete(@Param("id") Long id);
}
