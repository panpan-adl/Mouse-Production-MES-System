package com.mouse.mes.mapper;

import com.mouse.mes.entity.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MaterialMapper {
    List<Material> findAll();
    List<Material> findByTypes(@Param("list") List<Integer> types);
    Material findById(@Param("id") Long id);
    Material findByMaterialCode(@Param("materialCode") String materialCode);
    int insert(Material material);
    int update(Material material);
    int delete(@Param("id") Long id);
    int updateStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}