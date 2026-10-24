package com.mouse.mes.mapper;

import com.mouse.mes.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {
    List<Product> findAll();
    Product findById(@Param("id") Long id);
    Product findByProductCode(@Param("productCode") String productCode);
    int insert(Product product);
    int update(Product product);
    int delete(@Param("id") Long id);
}