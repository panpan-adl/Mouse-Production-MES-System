package com.mouse.mes.mapper;

import com.mouse.mes.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkOrderMapper {
    List<WorkOrder> findAll();
    WorkOrder findById(@Param("id") Long id);
    WorkOrder findByWoNo(@Param("woNo") String woNo);
    List<WorkOrder> findByProductionOrderId(@Param("productionOrderId") Long productionOrderId);
    int insert(WorkOrder workOrder);
    int update(WorkOrder workOrder);
    int delete(@Param("id") Long id);
    List<WorkOrder> findByStatus(@Param("status") String status);
    List<WorkOrder> findByAssignedUserId(@Param("assignedUserId") Long assignedUserId);
}
