package com.mouse.mes.controller;

import com.mouse.mes.entity.WorkOrder;
import com.mouse.mes.service.WorkOrderService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/work-order")
public class WorkOrderController {

    @Autowired
    private WorkOrderService workOrderService;

    @GetMapping("/list")
    public JsonResult<List<WorkOrder>> findAll() {
        return JsonResult.success(workOrderService.findAll());
    }

    @GetMapping("/{id}")
    public JsonResult<WorkOrder> findById(@PathVariable Long id) {
        WorkOrder workOrder = workOrderService.findById(id);
        if (workOrder == null) {
            return JsonResult.error("工单不存在");
        }
        return JsonResult.success(workOrder);
    }

    @GetMapping("/no/{woNo}")
    public JsonResult<WorkOrder> findByWoNo(@PathVariable String woNo) {
        WorkOrder workOrder = workOrderService.findByWoNo(woNo);
        if (workOrder == null) {
            return JsonResult.error("工单不存在");
        }
        return JsonResult.success(workOrder);
    }

    @GetMapping("/production/{productionOrderId}")
    public JsonResult<List<WorkOrder>> findByProductionOrderId(@PathVariable Long productionOrderId) {
        return JsonResult.success(workOrderService.findByProductionOrderId(productionOrderId));
    }

    @PostMapping("/add")
    public JsonResult<WorkOrder> create(@RequestBody WorkOrder workOrder) {
        if (workOrder.getWoNo() == null || workOrder.getWoNo().isEmpty()) {
            return JsonResult.error("工单编号不能为空");
        }
        if (workOrder.getProductionOrderId() == null) {
            return JsonResult.error("生产订单ID不能为空");
        }
        return JsonResult.success(workOrderService.create(workOrder));
    }

    @PutMapping("/{id}")
    public JsonResult<WorkOrder> update(@PathVariable Long id, @RequestBody WorkOrder workOrder) {
        WorkOrder existing = workOrderService.findById(id);
        if (existing == null) {
            return JsonResult.error("工单不存在");
        }
        workOrder.setId(id);
        return JsonResult.success(workOrderService.update(workOrder));
    }

    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Long id) {
        WorkOrder existing = workOrderService.findById(id);
        if (existing == null) {
            return JsonResult.error("工单不存在");
        }
        workOrderService.delete(id);
        return JsonResult.success();
    }

    @GetMapping("/status/{status}")
    public JsonResult<List<WorkOrder>> findByStatus(@PathVariable String status) {
        return JsonResult.success(workOrderService.findByStatus(status));
    }

    @GetMapping("/user/{userId}")
    public JsonResult<List<WorkOrder>> findByAssignedUserId(@PathVariable Long userId) {
        return JsonResult.success(workOrderService.findByAssignedUserId(userId));
    }

    @PutMapping("/{id}/assign")
    public JsonResult<WorkOrder> assignWorkOrder(@PathVariable Long id, @RequestBody AssignRequest request) {
        try {
            return JsonResult.success(workOrderService.assignWorkOrder(id, request.getUserId(), request.getUserName()));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/claim")
    public JsonResult<WorkOrder> claimWorkOrder(@PathVariable Long id, @RequestBody ClaimRequest request) {
        try {
            return JsonResult.success(workOrderService.claimWorkOrder(id, request.getUserId()));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    public JsonResult<WorkOrder> completeWorkOrder(@PathVariable Long id, @RequestBody CompleteRequest request) {
        try {
            return JsonResult.success(workOrderService.completeWorkOrder(id, request.getActualQuantity()));
        } catch (RuntimeException e) {
            return JsonResult.error(e.getMessage());
        }
    }

    static class AssignRequest {
        private Long userId;
        private String userName;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    static class ClaimRequest {
        private Long userId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }

    static class CompleteRequest {
        private BigDecimal actualQuantity;

        public BigDecimal getActualQuantity() {
            return actualQuantity;
        }

        public void setActualQuantity(BigDecimal actualQuantity) {
            this.actualQuantity = actualQuantity;
        }
    }
}
