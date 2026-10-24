package com.mouse.mes.controller;

import com.mouse.mes.entity.Order;
import com.mouse.mes.service.OrderService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public JsonResult<List<Order>> getAllOrders() {
        return JsonResult.success(orderService.findAll());
    }

    @GetMapping("/{id}")
    public JsonResult<Order> getOrderById(@PathVariable Long id) {
        Order order = orderService.findById(id);
        if (order == null) {
            return JsonResult.error("订单不存在");
        }
        return JsonResult.success(order);
    }

    @GetMapping("/orderno/{orderNo}")
    public JsonResult<Order> getOrderByOrderNo(@PathVariable String orderNo) {
        Order order = orderService.findByOrderNo(orderNo);
        if (order == null) {
            return JsonResult.error("订单不存在");
        }
        return JsonResult.success(order);
    }

    @GetMapping("/status/{status}")
    public JsonResult<List<Order>> getOrdersByStatus(@PathVariable Integer status) {
        return JsonResult.success(orderService.findByStatus(status));
    }

    @PostMapping
    public JsonResult<Order> createOrder(@RequestBody Order order, HttpServletRequest request) {
        if (order.getOrderNo() == null || order.getOrderNo().isEmpty()) {
            return JsonResult.error("订单编号不能为空");
        }
        if (order.getCustomerName() == null || order.getCustomerName().isEmpty()) {
            return JsonResult.error("客户名称不能为空");
        }
        if (order.getQuantity() == null || order.getQuantity() <= 0) {
            return JsonResult.error("数量必须大于0");
        }
        
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null && token.startsWith("TOKEN_")) {
            String[] parts = token.split("_");
            if (parts.length >= 2) {
                order.setCreateBy(Long.parseLong(parts[1]));
            }
        }
        
        return JsonResult.success(orderService.create(order));
    }

    @PutMapping("/{id}")
    public JsonResult<Order> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        Order existing = orderService.findById(id);
        if (existing == null) {
            return JsonResult.error("订单不存在");
        }
        order.setId(id);
        return JsonResult.success(orderService.update(order));
    }

    @DeleteMapping("/{id}")
    public JsonResult<Void> deleteOrder(@PathVariable Long id) {
        Order existing = orderService.findById(id);
        if (existing == null) {
            return JsonResult.error("订单不存在");
        }
        orderService.delete(id);
        return JsonResult.success();
    }

    @PutMapping("/{id}/confirm")
    public JsonResult<Order> confirmOrder(@PathVariable Long id) {
        try {
            return JsonResult.success(orderService.confirmOrder(id));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public JsonResult<Order> cancelOrder(@PathVariable Long id) {
        try {
            return JsonResult.success(orderService.cancelOrder(id));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }
}
