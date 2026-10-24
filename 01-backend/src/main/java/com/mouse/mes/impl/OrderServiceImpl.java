package com.mouse.mes.impl;

import com.mouse.mes.entity.Order;
import com.mouse.mes.mapper.OrderMapper;
import com.mouse.mes.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_CONFIRMED = 1;
    private static final int STATUS_PRODUCING = 2;
    private static final int STATUS_COMPLETED = 3;
    private static final int STATUS_CANCELLED = 4;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate ledgerDb;

    @Override
    public List<Order> findAll() {
        List<Order> orders = orderMapper.findAll();
        orders.forEach(this::normalizeOrderStatus);
        return orders;
    }

    @Override
    public Order findById(Long id) {
        Order order = orderMapper.findById(id);
        normalizeOrderStatus(order);
        return order;
    }

    @Override
    public Order findByOrderNo(String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        normalizeOrderStatus(order);
        return order;
    }

    @Override
    public Order create(Order order) {
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        order.setDeleted(0);
        if (order.getOrderDate() == null) {
            order.setOrderDate(new Date());
        }
        order.setOrderStatus(STATUS_PENDING);
        normalizeOrderStatus(order);
        if (order.getPriority() == null) {
            order.setPriority(2);
        }
        orderMapper.insert(order);
        return order;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Order update(Order order) {
        ledgerDb.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        if(ledgerDb.queryForObject("SELECT COUNT(*) FROM t_production_plan WHERE order_id=?",Integer.class,order.getId())>0) throw new IllegalArgumentException("订单已制定计划，不能修改产品、数量或完成状态");
        if(order.getOrderStatus()!=null && order.getOrderStatus()>1) throw new IllegalArgumentException("生产及发货完成状态由业务流程自动推进");
        Order existing = orderMapper.findById(order.getId());
        if (existing == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getOrderNo() != null) {
            existing.setOrderNo(order.getOrderNo());
        }
        if (order.getCustomerName() != null) {
            existing.setCustomerName(order.getCustomerName());
        }
        if (order.getCustomerPhone() != null) {
            existing.setCustomerPhone(order.getCustomerPhone());
        }
        if (order.getProductName() != null) {
            existing.setProductName(order.getProductName());
        }
        if (order.getQuantity() != null) {
            existing.setQuantity(order.getQuantity());
        }
        if (order.getUnitPrice() != null) {
            existing.setUnitPrice(order.getUnitPrice());
        }
        if (order.getTotalAmount() != null) {
            existing.setTotalAmount(order.getTotalAmount());
        }
        if (order.getOrderDate() != null) {
            existing.setOrderDate(order.getOrderDate());
        }
        if (order.getDeliveryDate() != null) {
            existing.setDeliveryDate(order.getDeliveryDate());
        }
        if (order.getOrderStatus() != null) {
            existing.setOrderStatus(order.getOrderStatus());
        }
        normalizeOrderStatus(existing);
        if (order.getPriority() != null) {
            existing.setPriority(order.getPriority());
        }
        if (order.getRemark() != null) {
            existing.setRemark(order.getRemark());
        }
        existing.setUpdateTime(new Date());
        orderMapper.update(existing);
        return findById(order.getId());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void delete(Long id) {
        ledgerDb.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        if(ledgerDb.queryForObject("SELECT COUNT(*) FROM t_production_plan WHERE order_id=?",Integer.class,id)>0) throw new IllegalArgumentException("订单已有生产计划，不能删除");
        orderMapper.delete(id);
    }

    @Override
    public List<Order> findByStatus(Integer status) {
        List<Order> orders = orderMapper.findByStatus(normalizeOrderStatus(status));
        orders.forEach(this::normalizeOrderStatus);
        return orders;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Order confirmOrder(Long id) {
        ledgerDb.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        normalizeOrderStatus(order);
        if (order.getOrderStatus() != STATUS_PENDING) {
            throw new RuntimeException("订单状态不允许确认");
        }
        orderMapper.updateStatus(id, STATUS_CONFIRMED, new Date());
        return findById(id);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Order cancelOrder(Long id) {
        ledgerDb.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        normalizeOrderStatus(order);
        if(ledgerDb.queryForObject("SELECT COUNT(*) FROM t_production_plan WHERE order_id=?",Integer.class,id)>0) throw new IllegalArgumentException("订单已有生产计划，不能直接取消");
        if (order.getOrderStatus() == STATUS_PRODUCING || order.getOrderStatus() == STATUS_COMPLETED) {
            throw new RuntimeException("订单已进入生产或已完成，无法取消");
        }
        orderMapper.updateStatus(id, STATUS_CANCELLED, new Date());
        return findById(id);
    }

    private void normalizeOrderStatus(Order order) {
        if (order == null) {
            return;
        }
        order.setOrderStatus(normalizeOrderStatus(order.getOrderStatus()));
    }

    private int normalizeOrderStatus(Integer status) {
        if (status == null) {
            return STATUS_PENDING;
        }
        if (status == STATUS_PENDING
                || status == STATUS_CONFIRMED
                || status == STATUS_PRODUCING
                || status == STATUS_COMPLETED
                || status == STATUS_CANCELLED) {
            return status;
        }
        return STATUS_PENDING;
    }
}
