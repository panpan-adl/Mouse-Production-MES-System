package com.mouse.mes.service;

import com.mouse.mes.entity.WorkOrder;

import java.math.BigDecimal;
import java.util.List;

public interface WorkOrderService {

    /**
     * 查询全部未删除工单。
     */
    List<WorkOrder> findAll();

    /**
     * 按工单ID查询工单。
     */
    WorkOrder findById(Long id);

    /**
     * 按工单编号查询工单。
     */
    WorkOrder findByWoNo(String woNo);

    /**
     * 按生产订单ID查询关联工单。
     */
    List<WorkOrder> findByProductionOrderId(Long productionOrderId);

    /**
     * 新增工单。
     */
    WorkOrder create(WorkOrder workOrder);

    /**
     * 修改工单基础信息。
     */
    WorkOrder update(WorkOrder workOrder);

    /**
     * 逻辑删除工单。
     */
    void delete(Long id);

    /**
     * 按工单状态查询工单。
     */
    List<WorkOrder> findByStatus(String status);

    /**
     * 按分配员工ID查询工单。
     */
    List<WorkOrder> findByAssignedUserId(Long assignedUserId);

    /**
     * 将工单分配给指定员工。
     */
    WorkOrder assignWorkOrder(Long id, Long userId, String userName);

    /**
     * 员工认领工单。
     */
    WorkOrder claimWorkOrder(Long id, Long userId);

    /**
     * 完成工单并记录实际完成数量。
     */
    WorkOrder completeWorkOrder(Long id, BigDecimal actualQuantity);
}
