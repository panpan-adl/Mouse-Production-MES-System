package com.mouse.mes.mapper;

import com.mouse.mes.entity.ProductionOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductionOrderMapper {
    List<ProductionOrder> findAll();
    ProductionOrder findById(@Param("id") Long id);
    ProductionOrder findByPoNo(@Param("poNo") String poNo);
    int insert(ProductionOrder productionOrder);
    int update(ProductionOrder productionOrder);
    int delete(@Param("id") Long id);
}
