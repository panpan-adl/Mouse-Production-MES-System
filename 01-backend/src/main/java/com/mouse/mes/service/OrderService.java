package com.mouse.mes.service;

import com.mouse.mes.entity.Order;

import java.util.List;

public interface OrderService {
    List<Order> findAll();
    Order findById(Long id);
    Order findByOrderNo(String orderNo);
    Order create(Order order);
    Order update(Order order);
    void delete(Long id);
    List<Order> findByStatus(Integer status);
    Order confirmOrder(Long id);
    Order cancelOrder(Long id);
}