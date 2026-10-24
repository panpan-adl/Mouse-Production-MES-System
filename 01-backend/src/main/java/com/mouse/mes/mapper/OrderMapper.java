package com.mouse.mes.mapper;

import com.mouse.mes.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface OrderMapper {
    List<Order> findAll();
    Order findById(@Param("id") Long id);
    Order findByOrderNo(@Param("orderNo") String orderNo);
    int insert(Order order);
    int update(Order order);
    int delete(@Param("id") Long id);
    List<Order> findByStatus(@Param("status") Integer status);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("updateTime") Date updateTime);
}