package com.mouse.mes.service;

import com.mouse.mes.entity.SalesOrder;

import java.util.List;
import java.util.Map;

public interface SalesOrderService {
    List<SalesOrder> findAll();
    SalesOrder findById(Long id);
    SalesOrder findBySoNo(String soNo);
    SalesOrder create(SalesOrder salesOrder);
    SalesOrder update(SalesOrder salesOrder);
    void delete(Long id);
    Map<String, Object> getStatistics();
}
