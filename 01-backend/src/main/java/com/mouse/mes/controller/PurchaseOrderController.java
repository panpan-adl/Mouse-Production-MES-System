package com.mouse.mes.controller;

import com.mouse.mes.dto.AcceptSalesOrderRequest;
import com.mouse.mes.dto.PurchaseOrderRequest;
import com.mouse.mes.dto.PurchaseReceiveRequest;
import com.mouse.mes.entity.PurchaseOrder;
import com.mouse.mes.service.PurchaseOrderService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-order")
public class PurchaseOrderController {

    /**
     * 采购订单业务服务，负责采购订单、明细、状态流转和到货入库。
     */
    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询全部未删除采购订单。
     */
    @GetMapping("/list")
    public JsonResult<List<PurchaseOrder>> findAll() {
        return JsonResult.success(purchaseOrderService.findAll());
    }

    /**
     * 按采购订单ID查询采购订单。
     */
    @GetMapping("/{id}")
    public JsonResult<PurchaseOrder> findById(@PathVariable Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderService.findById(id);
        if (purchaseOrder == null) {
            return JsonResult.error("采购订单不存在");
        }
        return JsonResult.success(purchaseOrder);
    }

    /**
     * 按采购订单编号查询采购订单。
     */
    @GetMapping("/no/{poNo}")
    public JsonResult<PurchaseOrder> findByPoNo(@PathVariable String poNo) {
        PurchaseOrder purchaseOrder = purchaseOrderService.findByPoNo(poNo);
        if (purchaseOrder == null) {
            return JsonResult.error("采购订单不存在");
        }
        return JsonResult.success(purchaseOrder);
    }

    /**
     * 按采购订单状态查询采购订单。
     */
    @GetMapping("/status/{status}")
    public JsonResult<List<PurchaseOrder>> findByStatus(@PathVariable String status) {
        return JsonResult.success(purchaseOrderService.findByStatus(status));
    }

    /**
     * 新增采购订单，可同时携带多个采购订单明细。
     */
    @PostMapping("/add")
    public JsonResult<PurchaseOrder> create(@RequestBody PurchaseOrderRequest request) {
        try {
            return JsonResult.success(purchaseOrderService.create(request));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 修改采购订单，可选择整体替换采购订单明细。
     */
    @PutMapping("/{id}")
    public JsonResult<PurchaseOrder> update(@PathVariable Long id, @RequestBody PurchaseOrderRequest request) {
        try {
            return JsonResult.success(purchaseOrderService.update(id, request));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 提交采购订单，将状态从 DRAFT 改为 SUBMITTED。
     */
    @PutMapping("/{id}/submit")
    public JsonResult<PurchaseOrder> submit(@PathVariable Long id) {
        try {
            return JsonResult.success(purchaseOrderService.submit(id));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 审核采购订单，将状态从 SUBMITTED 改为 APPROVED。
     */
    @PutMapping("/{id}/approve")
    public JsonResult<PurchaseOrder> approve(@PathVariable Long id) {
        try {
            return JsonResult.success(purchaseOrderService.approve(id));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 取消采购订单，将状态改为 CANCELLED。
     */
    @PutMapping("/{id}/cancel")
    public JsonResult<PurchaseOrder> cancel(@PathVariable Long id) {
        try {
            return JsonResult.success(purchaseOrderService.cancel(id));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 采购到货，校验数量后生成入库单并更新采购订单状态。
     */
    @PutMapping("/{id}/receive")
    public JsonResult<PurchaseOrder> receive(@PathVariable Long id, @RequestBody PurchaseReceiveRequest request) {
        try {
            return JsonResult.error("到货请进入生产与仓储闭环，完成来料检验后整批入库并登记库存流水");
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 逻辑删除采购订单，删除后查询列表不会再返回该订单。
     */
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Long id) {
        PurchaseOrder existing = purchaseOrderService.findById(id);
        if (existing == null) {
            return JsonResult.error("采购订单不存在");
        }
        purchaseOrderService.delete(id);
        return JsonResult.success();
    }

    /**
     * 接受销售订单并自动创建采购单。
     */
    @PostMapping("/accept-sales-order/{salesOrderId}")
    public JsonResult<PurchaseOrder> acceptSalesOrder(@PathVariable Long salesOrderId, @RequestBody AcceptSalesOrderRequest request) {
        try {
            return JsonResult.success(purchaseOrderService.createFromSalesOrder(salesOrderId, request.getSupplierId(), request.getExpectedDate()));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    @PostMapping("/migrate")
    public JsonResult<String> migrate() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'mouse_mes' AND TABLE_NAME = 't_purchase_order' AND COLUMN_NAME = 'sales_order_id'",
                Integer.class
            );
            if (count == null || count == 0) {
                jdbcTemplate.execute("ALTER TABLE t_purchase_order ADD COLUMN sales_order_id BIGINT COMMENT '关联的销售订单ID'");
            }
            return JsonResult.success("迁移成功");
        } catch (Exception e) {
            return JsonResult.error("迁移失败: " + e.getMessage());
        }
    }
}
