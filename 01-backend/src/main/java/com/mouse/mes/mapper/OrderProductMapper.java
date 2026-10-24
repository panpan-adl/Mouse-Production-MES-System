package com.mouse.mes.mapper;

import com.mouse.mes.entity.OrderProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderProductMapper {
    List<OrderProduct> findAll();
    OrderProduct findById(@Param("id") Long id);
    List<OrderProduct> findByOrderId(@Param("orderId") Long orderId);
    int insert(OrderProduct orderProduct);
    int update(OrderProduct orderProduct);
    int delete(@Param("id") Long id);
    int deleteByOrderId(@Param("orderId") Long orderId);
}