package com.mouse.mes.mapper;

import com.mouse.mes.entity.ProductionPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductionPlanMapper {
    List<ProductionPlan> findAll();
    ProductionPlan findById(@Param("id") Long id);
    ProductionPlan findByPlanNo(@Param("planNo") String planNo);
    int insert(ProductionPlan productionPlan);
    int update(ProductionPlan productionPlan);
    int delete(@Param("id") Long id);
    List<ProductionPlan> findByStatus(@Param("status") Integer status);
    List<ProductionPlan> findByOrderId(@Param("orderId") Long orderId);
}