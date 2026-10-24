package com.mouse.mes.impl;

import com.mouse.mes.entity.Order;
import com.mouse.mes.entity.ProductionPlan;
import com.mouse.mes.entity.SalesOrder;
import com.mouse.mes.entity.SalesOrderItem;
import com.mouse.mes.mapper.OrderMapper;
import com.mouse.mes.mapper.ProductionPlanMapper;
import com.mouse.mes.mapper.SalesOrderItemMapper;
import com.mouse.mes.mapper.SalesOrderMapper;
import com.mouse.mes.service.ProductionPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ProductionPlanServiceImpl implements ProductionPlanService {

    private static final int ORDER_STATUS_CONFIRMED = 1;
    @Autowired private com.mouse.mes.service.FoundationService foundation;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;
    private void validatePlan(ProductionPlan p) {
        if(p.getOrderId()!=null) {
            var orders=db.queryForList("SELECT * FROM t_order WHERE id=? AND deleted=0 FOR UPDATE",p.getOrderId());
            if(orders.isEmpty()) throw new IllegalArgumentException("来源订单不存在");
            var order=orders.get(0);
            if(com.mouse.mes.service.FoundationService.number(order.get("order_status"))>=2) throw new IllegalArgumentException("仅待确认或已确认订单可制定计划");
            var products=db.queryForList("SELECT id,product_name FROM t_product WHERE product_name=? AND status=1",order.get("product_name"));
            if(products.size()!=1) throw new IllegalArgumentException("请先建立与订单产品名称一致且唯一的产品档案");
            p.setProductId(com.mouse.mes.service.FoundationService.number(products.get(0).get("id")));
            p.setProductName(products.get(0).get("product_name").toString());
            p.setPlanQuantity(Math.toIntExact(com.mouse.mes.service.FoundationService.number(order.get("quantity"))));
        } else {
            var product=foundation.get("products",p.getProductId()==null?0:p.getProductId());
            p.setProductName(product.get("product_name").toString());
        }
        if(p.getPlanQuantity()==null||p.getPlanQuantity()<=0) throw new IllegalArgumentException("计划数量必须为正数");
        p.setPlanStatus(0);p.setCompletedQuantity(0);
    }
    private ProductionPlan canonicalOrderPlan(Long orderId) {
        Order order=orderMapper.findById(orderId);
        if(order==null) throw new IllegalArgumentException("销售订单不存在，请从订单列表选择来源");
        ProductionPlan plan=new ProductionPlan();plan.setPlanNo("PP"+System.currentTimeMillis());
        plan.setOrderId(orderId);plan.setStartDate(new Date());plan.setEndDate(order.getDeliveryDate());
        plan.setPriority(order.getPriority());plan.setCreateBy(order.getCreateBy());
        return create(plan);
    }

    @Autowired
    private ProductionPlanMapper productionPlanMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SalesOrderMapper salesOrderMapper;

    @Autowired
    private SalesOrderItemMapper salesOrderItemMapper;

    @Override
    public List<ProductionPlan> findAll() {
        return productionPlanMapper.findAll();
    }

    @Override
    public ProductionPlan findById(Long id) {
        return productionPlanMapper.findById(id);
    }

    @Override
    public ProductionPlan findByPlanNo(String planNo) {
        return productionPlanMapper.findByPlanNo(planNo);
    }

    @Override
    @Transactional
    public ProductionPlan create(ProductionPlan productionPlan) {
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        validatePlan(productionPlan);
        if (productionPlan.getOrderId() != null) {
            List<ProductionPlan> existingPlans = productionPlanMapper.findByOrderId(productionPlan.getOrderId());
            if (existingPlans != null && !existingPlans.isEmpty()) {
                throw new RuntimeException("该订单已制定生产计划");
            }
        }
        productionPlan.setCreateTime(new Date());
        productionPlan.setUpdateTime(new Date());
        if (productionPlan.getPlanStatus() == null) {
            productionPlan.setPlanStatus(0);
        }
        if (productionPlan.getCompletedQuantity() == null) {
            productionPlan.setCompletedQuantity(0);
        }
        if (productionPlan.getPriority() == null) {
            productionPlan.setPriority(2);
        }
        productionPlanMapper.insert(productionPlan);
        markOrderConfirmed(productionPlan.getOrderId());
        return productionPlan;
    }

    @Override
    @Transactional
    public ProductionPlan update(ProductionPlan productionPlan) {
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        foundation.protectPlan(productionPlan.getId());
        if(db.queryForObject("SELECT COUNT(*) FROM t_production_task WHERE plan_id=?",Integer.class,productionPlan.getId())>0) throw new IllegalArgumentException("已有任务的计划不能修改");
        ProductionPlan existing = productionPlanMapper.findById(productionPlan.getId());
        if (existing == null) {
            throw new RuntimeException("生产计划不存在");
        }
        if (productionPlan.getPlanNo() != null) {
            existing.setPlanNo(productionPlan.getPlanNo());
        }
        if (productionPlan.getOrderId() != null) {
            existing.setOrderId(productionPlan.getOrderId());
        }
        if (productionPlan.getProductId() != null) {
            existing.setProductId(productionPlan.getProductId());
        }
        if (productionPlan.getProductName() != null) {
            existing.setProductName(productionPlan.getProductName());
        }
        if (productionPlan.getPlanQuantity() != null) {
            existing.setPlanQuantity(productionPlan.getPlanQuantity());
        }
        if (productionPlan.getCompletedQuantity() != null) {
            existing.setCompletedQuantity(productionPlan.getCompletedQuantity());
        }
        if (productionPlan.getStartDate() != null) {
            existing.setStartDate(productionPlan.getStartDate());
        }
        if (productionPlan.getEndDate() != null) {
            existing.setEndDate(productionPlan.getEndDate());
        }
        if (productionPlan.getPlanStatus() != null) {
            existing.setPlanStatus(productionPlan.getPlanStatus());
        }
        if (productionPlan.getPriority() != null) {
            existing.setPriority(productionPlan.getPriority());
        }
        existing.setUpdateTime(new Date());
        productionPlanMapper.update(existing);
        return productionPlanMapper.findById(productionPlan.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        foundation.protectPlan(id);
        if(db.queryForObject("SELECT COUNT(*) FROM t_production_task WHERE plan_id=?",Integer.class,id)>0) throw new IllegalArgumentException("已有任务的计划不能删除");
        productionPlanMapper.delete(id);
    }

    @Override
    public List<ProductionPlan> findByStatus(Integer status) {
        return productionPlanMapper.findByStatus(status);
    }

    @Override
    public List<ProductionPlan> findByOrderId(Long orderId) {
        return productionPlanMapper.findByOrderId(orderId);
    }

    @Override
    @Transactional
    public ProductionPlan createFromOrder(Long orderId) {
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        return canonicalOrderPlan(orderId);
    }

    /**
     * 生产计划创建成功后，来源销售订单进入“已确认”状态。
     */
    private void markOrderConfirmed(Long orderId) {
        if (orderId != null && orderMapper.findById(orderId) != null) {
            orderMapper.updateStatus(orderId, ORDER_STATUS_CONFIRMED, new Date());
        }
    }
    @Override
    public ProductionPlan getProgress(Long id) {
        ProductionPlan plan = productionPlanMapper.findById(id);
        if (plan == null) {
            throw new RuntimeException("生产计划不存在");
        }
        return plan;
    }
}