package com.mouse.mes.service;

import com.mouse.mes.entity.ProductionOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ProductionOrderServiceTest {

    @Autowired
    private ProductionOrderService productionOrderService;

    @Test
    public void testFindAll() {
        List<ProductionOrder> orders = productionOrderService.findAll();
        assertNotNull(orders);
    }

    @Test
    public void testFindById() {
        List<ProductionOrder> orders = productionOrderService.findAll();
        if (!orders.isEmpty()) {
            Long id = orders.get(0).getId();
            ProductionOrder po = productionOrderService.findById(id);
            assertNotNull(po);
            assertEquals(id, po.getId());
        }
    }

    @Test
    public void testFindByPoNo() {
        List<ProductionOrder> orders = productionOrderService.findAll();
        if (!orders.isEmpty()) {
            String poNo = orders.get(0).getPoNo();
            ProductionOrder po = productionOrderService.findByPoNo(poNo);
            assertNotNull(po);
            assertEquals(poNo, po.getPoNo());
        }
    }

    @Test
    public void testCreateProductionOrder() {
        ProductionOrder po = new ProductionOrder();
        po.setPoNo("PO_SERVICE_TEST" + System.currentTimeMillis());
        po.setProductId(1L);
        po.setPlannedQuantity(new BigDecimal("100"));
        po.setStatus("PLANNED");
        po.setCreatedBy(1L);

        ProductionOrder created = productionOrderService.create(po);
        assertNotNull(created);
        assertEquals(po.getPoNo(), created.getPoNo());
        assertEquals("PLANNED", created.getStatus());
        assertEquals(0, created.getIsDeleted());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());
    }

    @Test
    public void testUpdateProductionOrder() {
        ProductionOrder po = new ProductionOrder();
        po.setPoNo("PO_UPDATE_TEST" + System.currentTimeMillis());
        po.setProductId(1L);
        po.setPlannedQuantity(new BigDecimal("100"));
        po.setStatus("PLANNED");
        po.setCreatedBy(1L);

        ProductionOrder created = productionOrderService.create(po);
        ProductionOrder saved = productionOrderService.findByPoNo(po.getPoNo());
        assertNotNull(saved);

        saved.setStatus("IN_PROGRESS");
        ProductionOrder updated = productionOrderService.update(saved);
        assertNotNull(updated);
        assertEquals("IN_PROGRESS", updated.getStatus());
    }

    @Test
    public void testDeleteProductionOrder() {
        ProductionOrder po = new ProductionOrder();
        po.setPoNo("PO_DELETE_TEST" + System.currentTimeMillis());
        po.setProductId(1L);
        po.setPlannedQuantity(new BigDecimal("50"));
        po.setStatus("PLANNED");
        po.setCreatedBy(1L);

        productionOrderService.create(po);
        ProductionOrder saved = productionOrderService.findByPoNo(po.getPoNo());
        assertNotNull(saved);

        productionOrderService.delete(saved.getId());

        ProductionOrder deleted = productionOrderService.findById(saved.getId());
        assertNull(deleted);
    }
}