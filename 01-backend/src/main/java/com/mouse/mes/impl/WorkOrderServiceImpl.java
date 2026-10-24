package com.mouse.mes.impl;

import com.mouse.mes.entity.WorkOrder;
import com.mouse.mes.mapper.WorkOrderMapper;
import com.mouse.mes.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    /**
     * 工单数据库访问对象。
     */
    @Autowired
    private WorkOrderMapper workOrderMapper;

    /**
     * 查询全部未删除工单。
     */
    @Override
    public List<WorkOrder> findAll() {
        return workOrderMapper.findAll();
    }

    /**
     * 按工单ID查询工单。
     */
    @Override
    public WorkOrder findById(Long id) {
        return workOrderMapper.findById(id);
    }

    /**
     * 按工单编号查询工单。
     */
    @Override
    public WorkOrder findByWoNo(String woNo) {
        return workOrderMapper.findByWoNo(woNo);
    }

    /**
     * 按生产订单ID查询关联工单。
     */
    @Override
    public List<WorkOrder> findByProductionOrderId(Long productionOrderId) {
        return workOrderMapper.findByProductionOrderId(productionOrderId);
    }

    /**
     * 新增工单。
     */
    @Override
    public WorkOrder create(WorkOrder workOrder) {
        if (workOrder.getStatus() == null || workOrder.getStatus().isBlank()) {
            workOrder.setStatus("PENDING");
        }
        workOrder.setCreatedAt(new Date());
        workOrder.setUpdatedAt(new Date());
        workOrder.setIsDeleted(0);
        workOrderMapper.insert(workOrder);
        return workOrderMapper.findById(workOrder.getId());
    }

    /**
     * 修改工单基础信息。
     */
    @Override
    public WorkOrder update(WorkOrder workOrder) {
        WorkOrder existing = workOrderMapper.findById(workOrder.getId());
        if (existing == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        mergeMissingFields(workOrder, existing);
        workOrder.setUpdatedAt(new Date());
        workOrderMapper.update(workOrder);
        return workOrderMapper.findById(workOrder.getId());
    }

    /**
     * 逻辑删除工单。
     */
    @Override
    public void delete(Long id) {
        WorkOrder existing = workOrderMapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        workOrderMapper.delete(id);
    }

    /**
     * 按工单状态查询工单。
     */
    @Override
    public List<WorkOrder> findByStatus(String status) {
        return workOrderMapper.findByStatus(status);
    }

    /**
     * 按分配员工ID查询工单。
     */
    @Override
    public List<WorkOrder> findByAssignedUserId(Long assignedUserId) {
        return workOrderMapper.findByAssignedUserId(assignedUserId);
    }

    /**
     * 将工单分配给指定员工，并把状态改为已分配。
     */
    @Override
    public WorkOrder assignWorkOrder(Long id, Long userId, String userName) {
        WorkOrder workOrder = requireWorkOrder(id);
        workOrder.setAssignedUserId(userId);
        workOrder.setAssignedUserName(userName);
        workOrder.setStatus("ASSIGNED");
        workOrder.setUpdatedAt(new Date());
        workOrderMapper.update(workOrder);
        return workOrderMapper.findById(id);
    }

    /**
     * 员工认领工单，并把状态改为进行中。
     */
    @Override
    public WorkOrder claimWorkOrder(Long id, Long userId) {
        WorkOrder workOrder = requireWorkOrder(id);
        workOrder.setAssignedUserId(userId);
        workOrder.setStatus("IN_PROGRESS");
        workOrder.setActualStartDate(new Date());
        workOrder.setUpdatedAt(new Date());
        workOrderMapper.update(workOrder);
        return workOrderMapper.findById(id);
    }

    /**
     * 完成工单并记录实际数量。
     */
    @Override
    public WorkOrder completeWorkOrder(Long id, BigDecimal actualQuantity) {
        WorkOrder workOrder = requireWorkOrder(id);
        workOrder.setActualQuantity(actualQuantity);
        workOrder.setStatus("COMPLETED");
        workOrder.setActualEndDate(new Date());
        workOrder.setUpdatedAt(new Date());
        workOrderMapper.update(workOrder);
        return workOrderMapper.findById(id);
    }

    /**
     * 查询工单；不存在时抛出业务错误。
     */
    private WorkOrder requireWorkOrder(Long id) {
        WorkOrder workOrder = workOrderMapper.findById(id);
        if (workOrder == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        return workOrder;
    }

    /**
     * 修改工单时补齐没有传入的字段，避免把旧数据覆盖为空。
     */
    private void mergeMissingFields(WorkOrder target, WorkOrder existing) {
        if (target.getWoNo() == null || target.getWoNo().isBlank()) {
            target.setWoNo(existing.getWoNo());
        }
        if (target.getProductionOrderId() == null) {
            target.setProductionOrderId(existing.getProductionOrderId());
        }
        if (target.getProductId() == null) {
            target.setProductId(existing.getProductId());
        }
        if (target.getPlannedQuantity() == null) {
            target.setPlannedQuantity(existing.getPlannedQuantity());
        }
        if (target.getActualQuantity() == null) {
            target.setActualQuantity(existing.getActualQuantity());
        }
        if (target.getPlannedStartDate() == null) {
            target.setPlannedStartDate(existing.getPlannedStartDate());
        }
        if (target.getPlannedEndDate() == null) {
            target.setPlannedEndDate(existing.getPlannedEndDate());
        }
        if (target.getActualStartDate() == null) {
            target.setActualStartDate(existing.getActualStartDate());
        }
        if (target.getActualEndDate() == null) {
            target.setActualEndDate(existing.getActualEndDate());
        }
        if (target.getStatus() == null || target.getStatus().isBlank()) {
            target.setStatus(existing.getStatus());
        }
        if (target.getDepartmentId() == null) {
            target.setDepartmentId(existing.getDepartmentId());
        }
        if (target.getAssignedUserId() == null) {
            target.setAssignedUserId(existing.getAssignedUserId());
        }
        if (target.getAssignedUserName() == null) {
            target.setAssignedUserName(existing.getAssignedUserName());
        }
        if (target.getCreatedBy() == null) {
            target.setCreatedBy(existing.getCreatedBy());
        }
        target.setCreatedAt(existing.getCreatedAt());
        target.setIsDeleted(existing.getIsDeleted());
    }
}
