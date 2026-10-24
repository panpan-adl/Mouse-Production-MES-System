package com.mouse.mes.service;

import com.mouse.mes.entity.WorkOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class WorkOrderServiceTest {

    @Autowired
    private WorkOrderService workOrderService;

    @Test
    public void testFindAll() {
        List<WorkOrder> workOrders = workOrderService.findAll();
        assertNotNull(workOrders);
    }

    @Test
    public void testFindById() {
        List<WorkOrder> workOrders = workOrderService.findAll();
        if (!workOrders.isEmpty()) {
            Long id = workOrders.get(0).getId();
            WorkOrder wo = workOrderService.findById(id);
            assertNotNull(wo);
            assertEquals(id, wo.getId());
        }
    }

    @Test
    public void testFindByWoNo() {
        List<WorkOrder> workOrders = workOrderService.findAll();
        if (!workOrders.isEmpty()) {
            String woNo = workOrders.get(0).getWoNo();
            WorkOrder wo = workOrderService.findByWoNo(woNo);
            assertNotNull(wo);
            assertEquals(woNo, wo.getWoNo());
        }
    }

    @Test
    public void testCreateWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo("WO_SERVICE_TEST" + System.currentTimeMillis());
        workOrder.setProductionOrderId(1L);
        workOrder.setProductId(1L);
        workOrder.setPlannedQuantity(new BigDecimal("100"));
        workOrder.setCreatedBy(1L);

        WorkOrder created = workOrderService.create(workOrder);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(workOrder.getWoNo(), created.getWoNo());
        assertEquals("PLANNED", created.getStatus());
        assertEquals(0, created.getIsDeleted());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());
    }

    @Test
    public void testAssignWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo("WO_ASSIGN_SERVICE" + System.currentTimeMillis());
        workOrder.setProductionOrderId(1L);
        workOrder.setProductId(1L);
        workOrder.setPlannedQuantity(new BigDecimal("50"));
        workOrder.setCreatedBy(1L);

        WorkOrder created = workOrderService.create(workOrder);
        WorkOrder assigned = workOrderService.assignWorkOrder(created.getId(), 1L, "张三");

        assertNotNull(assigned);
        assertEquals("ASSIGNED", assigned.getStatus());
        assertEquals(1L, assigned.getAssignedUserId());
        assertEquals("张三", assigned.getAssignedUserName());
    }

    @Test
    public void testAssignWorkOrderNotPlanned() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo("WO_ASSIGN_FAIL" + System.currentTimeMillis());
        workOrder.setProductionOrderId(1L);
        workOrder.setProductId(1L);
        workOrder.setPlannedQuantity(new BigDecimal("30"));
        workOrder.setCreatedBy(1L);

        WorkOrder created = workOrderService.create(workOrder);
        workOrderService.assignWorkOrder(created.getId(), 1L, "张三");

        assertThrows(RuntimeException.class, () -> workOrderService.assignWorkOrder(created.getId(), 2L, "李四"));
    }

    @Test
    public void testClaimWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo("WO_CLAIM_SERVICE" + System.currentTimeMillis());
        workOrder.setProductionOrderId(1L);
        workOrder.setProductId(1L);
        workOrder.setPlannedQuantity(new BigDecimal("30"));
        workOrder.setCreatedBy(1L);

        WorkOrder created = workOrderService.create(workOrder);
        workOrderService.assignWorkOrder(created.getId(), 1L, "张三");
        WorkOrder claimed = workOrderService.claimWorkOrder(created.getId(), 1L);

        assertNotNull(claimed);
        assertEquals("IN_PROGRESS", claimed.getStatus());
        assertNotNull(claimed.getActualStartDate());
    }

    @Test
    public void testClaimWorkOrderWrongUser() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo("WO_CLAIM_WRONG" + System.currentTimeMillis());
        workOrder.setProductionOrderId(1L);
        workOrder.setProductId(1L);
        workOrder.setPlannedQuantity(new BigDecimal("30"));
        workOrder.setCreatedBy(1L);

        WorkOrder created = workOrderService.create(workOrder);
        workOrderService.assignWorkOrder(created.getId(), 1L, "张三");

        assertThrows(RuntimeException.class, () -> workOrderService.claimWorkOrder(created.getId(), 2L));
    }

    @Test
    public void testCompleteWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo("WO_COMPLETE_SERVICE" + System.currentTimeMillis());
        workOrder.setProductionOrderId(1L);
        workOrder.setProductId(1L);
        workOrder.setPlannedQuantity(new BigDecimal("20"));
        workOrder.setCreatedBy(1L);

        WorkOrder created = workOrderService.create(workOrder);
        workOrderService.assignWorkOrder(created.getId(), 1L, "张三");
        workOrderService.claimWorkOrder(created.getId(), 1L);
        WorkOrder completed = workOrderService.completeWorkOrder(created.getId(), new BigDecimal("20"));

        assertNotNull(completed);
        assertEquals("COMPLETED", completed.getStatus());
        assertEquals(0, new BigDecimal("20").compareTo(completed.getActualQuantity()));
        assertNotNull(completed.getActualEndDate());
    }

    @Test
    public void testCompleteWorkOrderNotInProgress() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo("WO_COMPLETE_FAIL" + System.currentTimeMillis());
        workOrder.setProductionOrderId(1L);
        workOrder.setProductId(1L);
        workOrder.setPlannedQuantity(new BigDecimal("30"));
        workOrder.setCreatedBy(1L);

        WorkOrder created = workOrderService.create(workOrder);

        assertThrows(RuntimeException.class, () -> workOrderService.completeWorkOrder(created.getId(), new BigDecimal("30")));
    }

    @Test
    public void testFindByStatus() {
        List<WorkOrder> plannedOrders = workOrderService.findByStatus("PLANNED");
        assertNotNull(plannedOrders);
    }

    @Test
    public void testFindByAssignedUserId() {
        List<WorkOrder> userOrders = workOrderService.findByAssignedUserId(1L);
        assertNotNull(userOrders);
    }

    @Test
    public void testFindByProductionOrderId() {
        List<WorkOrder> poOrders = workOrderService.findByProductionOrderId(1L);
        assertNotNull(poOrders);
    }

    @Test
    public void testUpdateWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo("WO_UPDATE_SERVICE" + System.currentTimeMillis());
        workOrder.setProductionOrderId(1L);
        workOrder.setProductId(1L);
        workOrder.setPlannedQuantity(new BigDecimal("100"));
        workOrder.setCreatedBy(1L);

        WorkOrder created = workOrderService.create(workOrder);
        created.setPlannedQuantity(new BigDecimal("150"));

        WorkOrder updated = workOrderService.update(created);
        assertNotNull(updated);
        assertEquals(0, new BigDecimal("150").compareTo(updated.getPlannedQuantity()));
    }

    @Test
    public void testDeleteWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo("WO_DELETE_SERVICE" + System.currentTimeMillis());
        workOrder.setProductionOrderId(1L);
        workOrder.setProductId(1L);
        workOrder.setPlannedQuantity(new BigDecimal("50"));
        workOrder.setCreatedBy(1L);

        WorkOrder created = workOrderService.create(workOrder);
        workOrderService.delete(created.getId());

        assertNull(workOrderService.findById(created.getId()));
    }
}