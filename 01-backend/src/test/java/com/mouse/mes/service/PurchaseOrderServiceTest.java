package com.mouse.mes.service;

import com.mouse.mes.constant.PurchaseOrderStatus;
import com.mouse.mes.dto.PurchaseOrderRequest;
import com.mouse.mes.dto.PurchaseReceiveRequest;
import com.mouse.mes.entity.PurchaseOrder;
import com.mouse.mes.entity.PurchaseOrderItem;
import com.mouse.mes.entity.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class PurchaseOrderServiceTest {

    /**
     * 采购订单业务服务，用于测试采购订单新增、修改、删除、查询、状态流转和到货。
     */
    @Autowired
    private PurchaseOrderService purchaseOrderService;

    /**
     * 供应商业务服务，用于创建采购订单测试所需的供应商。
     */
    @Autowired
    private SupplierService supplierService;

    /**
     * 测试新增采购订单，新增后数据库中应能查询到。
     */
    @Test
    public void testAddPurchaseOrder() {
        PurchaseOrder savedOrder = createDraftOrder("ADD", new BigDecimal("10"));
        PurchaseOrder dbOrder = purchaseOrderService.findById(savedOrder.getId());
        System.out.println("新增采购订单结果：" + dbOrder);
        assertNotNull(dbOrder);
        assertEquals(savedOrder.getPoNo(), dbOrder.getPoNo());
        assertEquals(PurchaseOrderStatus.DRAFT, dbOrder.getStatus());
        assertFalse(dbOrder.getItems().isEmpty());
    }

    /**
     * 测试修改采购订单，修改后数据库字段应发生变化。
     */
    @Test
    public void testUpdatePurchaseOrder() {
        PurchaseOrder savedOrder = createDraftOrder("UPDATE", new BigDecimal("10"));
        PurchaseOrderRequest request = new PurchaseOrderRequest();
        request.setRemark("采购订单修改测试");
        PurchaseOrder updatedOrder = purchaseOrderService.update(savedOrder.getId(), request);
        System.out.println("修改采购订单结果：" + updatedOrder);
        assertNotNull(updatedOrder);
        assertEquals("采购订单修改测试", updatedOrder.getRemark());
    }

    /**
     * 测试按采购订单编号查询采购订单。
     */
    @Test
    public void testFindByPoNo() {
        PurchaseOrder savedOrder = createDraftOrder("FIND-NO", new BigDecimal("10"));
        PurchaseOrder dbOrder = purchaseOrderService.findByPoNo(savedOrder.getPoNo());
        System.out.println("按订单号查询采购订单结果：" + dbOrder);
        assertNotNull(dbOrder);
        assertEquals(savedOrder.getPoNo(), dbOrder.getPoNo());
    }

    /**
     * 测试按状态查询采购订单。
     */
    @Test
    public void testFindByStatus() {
        PurchaseOrder savedOrder = createDraftOrder("STATUS", new BigDecimal("10"));
        List<PurchaseOrder> draftOrders = purchaseOrderService.findByStatus(PurchaseOrderStatus.DRAFT);
        boolean exists = draftOrders.stream().anyMatch(order -> order.getId().equals(savedOrder.getId()));
        System.out.println("草稿采购订单数量：" + draftOrders.size());
        assertTrue(exists);
    }

    /**
     * 测试删除采购订单，删除后查询所有订单时不应再出现。
     */
    @Test
    public void testDeletePurchaseOrderAndListCanNotFindIt() {
        PurchaseOrder savedOrder = createDraftOrder("DELETE", new BigDecimal("10"));
        purchaseOrderService.delete(savedOrder.getId());
        List<PurchaseOrder> orderList = purchaseOrderService.findAll();
        boolean exists = orderList.stream().anyMatch(order -> order.getId().equals(savedOrder.getId()));
        System.out.println("删除后是否还能在列表中查到：" + exists);
        assertFalse(exists);
    }

    /**
     * 测试采购到货后的部分到货状态。
     */
    @Test
    public void testReceivePartialPurchaseOrder() {
        PurchaseOrder approvedOrder = approveOrder(createDraftOrder("PARTIAL", new BigDecimal("10")));
        PurchaseReceiveRequest request = buildReceiveRequest(approvedOrder, new BigDecimal("4"));
        PurchaseOrder receivedOrder = purchaseOrderService.receive(approvedOrder.getId(), request);
        System.out.println("部分到货后采购订单状态：" + receivedOrder.getStatus());
        assertEquals(PurchaseOrderStatus.PARTIAL_RECEIVED, receivedOrder.getStatus());
    }

    /**
     * 测试采购到货后的全部到货状态。
     */
    @Test
    public void testReceiveCompletedPurchaseOrder() {
        PurchaseOrder approvedOrder = approveOrder(createDraftOrder("COMPLETE", new BigDecimal("5")));
        PurchaseReceiveRequest request = buildReceiveRequest(approvedOrder, new BigDecimal("5"));
        PurchaseOrder receivedOrder = purchaseOrderService.receive(approvedOrder.getId(), request);
        System.out.println("全部到货后采购订单状态：" + receivedOrder.getStatus());
        assertEquals(PurchaseOrderStatus.COMPLETED, receivedOrder.getStatus());
    }

    /**
     * 测试超量到货应失败。
     */
    @Test
    public void testOverReceiveShouldFail() {
        PurchaseOrder approvedOrder = approveOrder(createDraftOrder("OVER", new BigDecimal("5")));
        PurchaseReceiveRequest request = buildReceiveRequest(approvedOrder, new BigDecimal("6"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            purchaseOrderService.receive(approvedOrder.getId(), request);
        });
        System.out.println("超量到货失败提示：" + exception.getMessage());
        assertEquals("到货数量不能超过采购数量", exception.getMessage());
    }

    /**
     * 创建草稿采购订单。
     */
    private PurchaseOrder createDraftOrder(String scene, BigDecimal quantity) {
        Supplier supplier = createSupplier(scene);
        PurchaseOrderRequest request = new PurchaseOrderRequest();
        request.setPoNo("PO-" + scene + "-" + System.nanoTime());
        request.setSupplierId(supplier.getId());
        request.setOrderDate(new Date());
        request.setExpectedDate(new Date());
        request.setStatus(PurchaseOrderStatus.DRAFT);
        request.setRemark("采购订单测试数据");
        request.setCreatedBy(1L);
        request.setItems(Collections.singletonList(buildOrderItem(quantity)));
        return purchaseOrderService.create(request);
    }

    /**
     * 创建采购订单测试所需的供应商。
     */
    private Supplier createSupplier(String scene) {
        Supplier supplier = new Supplier();
        supplier.setSupplierCode("SUP-PO-" + scene + "-" + System.nanoTime());
        supplier.setSupplierName("采购测试供应商-" + scene);
        supplier.setContactPerson("采购测试联系人");
        supplier.setContactPhone("13800000000");
        supplier.setStatus(1);
        return supplierService.create(supplier);
    }

    /**
     * 构造采购订单明细。
     */
    private PurchaseOrderItem buildOrderItem(BigDecimal quantity) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setMaterialId(1L);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal("10"));
        item.setAmount(quantity.multiply(new BigDecimal("10")));
        item.setRemark("采购明细测试数据");
        return item;
    }

    /**
     * 将草稿采购订单提交并审核。
     */
    private PurchaseOrder approveOrder(PurchaseOrder draftOrder) {
        purchaseOrderService.submit(draftOrder.getId());
        return purchaseOrderService.approve(draftOrder.getId());
    }

    /**
     * 构造采购到货请求。
     */
    private PurchaseReceiveRequest buildReceiveRequest(PurchaseOrder order, BigDecimal receiveQuantity) {
        PurchaseReceiveRequest request = new PurchaseReceiveRequest();
        request.setWarehouseId(1L);
        request.setCreatedBy(1L);
        request.setRemark("采购到货测试数据");
        PurchaseReceiveRequest.Item item = new PurchaseReceiveRequest.Item();
        item.setPurchaseOrderItemId(order.getItems().get(0).getId());
        item.setReceiveQuantity(receiveQuantity);
        item.setRemark("采购到货明细测试数据");
        request.setItems(Collections.singletonList(item));
        return request;
    }
}
