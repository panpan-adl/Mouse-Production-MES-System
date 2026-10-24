package com.mouse.mes.impl;

import com.mouse.mes.entity.Customer;
import com.mouse.mes.entity.SalesOrder;
import com.mouse.mes.mapper.CustomerMapper;
import com.mouse.mes.mapper.SalesOrderMapper;
import com.mouse.mes.service.SalesOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesOrderServiceImpl implements SalesOrderService {

    @Autowired
    private SalesOrderMapper salesOrderMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Override
    public List<SalesOrder> findAll() {
        return salesOrderMapper.findAll();
    }

    @Override
    public SalesOrder findById(Long id) {
        return salesOrderMapper.findById(id);
    }

    @Override
    public SalesOrder findBySoNo(String soNo) {
        return salesOrderMapper.findBySoNo(soNo);
    }

    @Override
    public SalesOrder create(SalesOrder salesOrder) {
        salesOrder.setCreatedAt(new Date());
        salesOrder.setUpdatedAt(new Date());
        salesOrder.setIsDeleted(0);
        if (salesOrder.getStatus() == null) {
            salesOrder.setStatus("PENDING");
        }
        if (salesOrder.getMaterialStatus() == null) {
            salesOrder.setMaterialStatus("PENDING");
        }
        salesOrderMapper.insert(salesOrder);
        return salesOrder;
    }

    @Override
    public SalesOrder update(SalesOrder salesOrder) {
        salesOrder.setUpdatedAt(new Date());
        salesOrderMapper.update(salesOrder);
        return salesOrderMapper.findById(salesOrder.getId());
    }

    @Override
    public void delete(Long id) {
        SalesOrder salesOrder = salesOrderMapper.findById(id);
        if (salesOrder != null) {
            salesOrder.setIsDeleted(1);
            salesOrder.setUpdatedAt(new Date());
            salesOrderMapper.update(salesOrder);
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        List<SalesOrder> orders = salesOrderMapper.findAll();
        List<Customer> customers = customerMapper.findAll();
        Map<Long, Customer> customerMap = customers.stream()
            .collect(Collectors.toMap(Customer::getId, c -> c));
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", orders.size());
        
        BigDecimal totalAmount = orders.stream()
            .map(SalesOrder::getTotalAmount)
            .filter(a -> a != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalAmount", totalAmount);
        
        long pendingCount = orders.stream()
            .filter(o -> "PENDING".equals(o.getStatus()) || "1".equals(o.getStatus()))
            .count();
        stats.put("pendingCount", pendingCount);
        
        long completedCount = orders.stream()
            .filter(o -> "COMPLETED".equals(o.getStatus()) || "3".equals(o.getStatus()))
            .count();
        stats.put("completedCount", completedCount);
        
        Map<Long, List<SalesOrder>> ordersByCustomer = orders.stream()
            .filter(o -> o.getCustomerId() != null)
            .collect(Collectors.groupingBy(SalesOrder::getCustomerId));
        
        List<Map<String, Object>> customerStats = new ArrayList<>();
        for (Map.Entry<Long, List<SalesOrder>> entry : ordersByCustomer.entrySet()) {
            Long customerId = entry.getKey();
            List<SalesOrder> customerOrders = entry.getValue();
            Customer customer = customerMap.get(customerId);
            
            Map<String, Object> customerStat = new HashMap<>();
            customerStat.put("customerId", customerId);
            customerStat.put("customerName", customer != null ? customer.getCustomerName() : "未知客户");
            customerStat.put("orderCount", customerOrders.size());
            BigDecimal orderAmount = customerOrders.stream()
                .map(SalesOrder::getTotalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            customerStat.put("orderAmount", orderAmount);
            customerStats.add(customerStat);
        }
        
        stats.put("customerStats", customerStats);
        
        return stats;
    }
}
