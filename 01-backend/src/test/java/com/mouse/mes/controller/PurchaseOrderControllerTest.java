package com.mouse.mes.controller;

import com.mouse.mes.constant.PurchaseOrderStatus;
import com.mouse.mes.dto.PurchaseOrderRequest;
import com.mouse.mes.dto.PurchaseReceiveRequest;
import com.mouse.mes.entity.PurchaseOrder;
import com.mouse.mes.entity.PurchaseOrderItem;
import com.mouse.mes.entity.Supplier;
import com.mouse.mes.service.SupplierService;
import com.mouse.mes.util.JsonResult;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PurchaseOrderControllerTest {

    /**
     * 采购订单控制器，用于测试采购订单接口返回结果。
     */
    @Autowired
    private PurchaseOrderController purchaseOrderController;

    /**
     * 供应商业务服务，用于创建采购订单接口测试所需的供应商。
     */
    @Autowired
    private SupplierService supplierService;

    /**
     * 测试新增采购订单接口。
     */
    @Test
    public void testAddPurchaseOrder() {
        JsonResult<PurchaseOrder> result = purchaseOrderController.create(buildOrderRequest("CTRL-ADD", new BigDecimal("10")));
        System.out.println("采购订单新增接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 测试修改采购订单接口。
     */
    @Test
    public void testUpdatePurchaseOrder() {
        PurchaseOrder savedOrder = purchaseOrderController.create(buildOrderRequest("CTRL-UPDATE", new BigDecimal("10"))).getData();
        PurchaseOrderRequest updateRequest = new PurchaseOrderRequest();
        updateRequest.setRemark("控制器修改采购订单");
        JsonResult<PurchaseOrder> result = purchaseOrderController.update(savedOrder.getId(), updateRequest);
        System.out.println("采购订单修改接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals("控制器修改采购订单", result.getData().getRemark());
    }

    /**
     * 测试删除不存在采购订单接口，应返回不存在。
     */
    @Test
    public void testDeleteNotExistPurchaseOrder() {
        JsonResult<Void> result = purchaseOrderController.delete(999999999L);
        System.out.println("删除不存在采购订单接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
        assertEquals("采购订单不存在", result.getMsg());
    }

    /**
     * 测试删除采购订单后，查询列表不能再查到该订单。
     */
    @Test
    public void testDeletedOrderCanNotBeFoundInList() {
        PurchaseOrder savedOrder = purchaseOrderController.create(buildOrderRequest("CTRL-DELETE", new BigDecimal("10"))).getData();
        purchaseOrderController.delete(savedOrder.getId());
        JsonResult<List<PurchaseOrder>> listResult = purchaseOrderController.findAll();
        boolean exists = listResult.getData().stream().anyMatch(order -> order.getId().equals(savedOrder.getId()));
        System.out.println("删除后列表是否还能查到采购订单：" + exists);
        assertFalse(exists);
    }

    /**
     * 测试采购订单部分到货接口。
     */
    @Test
    public void testReceivePartialPurchaseOrder() {
        PurchaseOrder approvedOrder = createApprovedOrder("CTRL-PARTIAL", new BigDecimal("10"));
        JsonResult<PurchaseOrder> result = purchaseOrderController.receive(
                approvedOrder.getId(),
                buildReceiveRequest(approvedOrder, new BigDecimal("4"))
        );
        System.out.println("部分到货接口结果：" + result.getData().getStatus());
        assertEquals(200, result.getCode());
        assertEquals(PurchaseOrderStatus.PARTIAL_RECEIVED, result.getData().getStatus());
    }

    /**
     * 测试采购订单全部到货接口。
     */
    @Test
    public void testReceiveCompletedPurchaseOrder() {
        PurchaseOrder approvedOrder = createApprovedOrder("CTRL-COMPLETE", new BigDecimal("5"));
        JsonResult<PurchaseOrder> result = purchaseOrderController.receive(
                approvedOrder.getId(),
                buildReceiveRequest(approvedOrder, new BigDecimal("5"))
        );
        System.out.println("全部到货接口结果：" + result.getData().getStatus());
        assertEquals(200, result.getCode());
        assertEquals(PurchaseOrderStatus.COMPLETED, result.getData().getStatus());
    }

    /**
     * 测试采购订单超量到货接口，应返回失败。
     */
    @Test
    public void testOverReceiveShouldFail() {
        PurchaseOrder approvedOrder = createApprovedOrder("CTRL-OVER", new BigDecimal("5"));
        JsonResult<PurchaseOrder> result = purchaseOrderController.receive(
                approvedOrder.getId(),
                buildReceiveRequest(approvedOrder, new BigDecimal("6"))
        );
        System.out.println("超量到货接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
        assertEquals("到货数量不能超过采购数量", result.getMsg());
    }

    /**
     * 创建并审核采购订单。
     */
    private PurchaseOrder createApprovedOrder(String scene, BigDecimal quantity) {
        PurchaseOrder order = purchaseOrderController.create(buildOrderRequest(scene, quantity)).getData();
        purchaseOrderController.submit(order.getId());
        return purchaseOrderController.approve(order.getId()).getData();
    }

    /**
     * 构造采购订单新增请求。
     */
    private PurchaseOrderRequest buildOrderRequest(String scene, BigDecimal quantity) {
        Supplier supplier = createSupplier(scene);
        PurchaseOrderRequest request = new PurchaseOrderRequest();
        request.setPoNo("PO-" + scene + "-" + System.nanoTime());
        request.setSupplierId(supplier.getId());
        request.setOrderDate(new Date());
        request.setExpectedDate(new Date());
        request.setStatus(PurchaseOrderStatus.DRAFT);
        request.setRemark("采购订单控制器测试数据");
        request.setCreatedBy(1L);
        request.setItems(Collections.singletonList(buildOrderItem(quantity)));
        return request;
    }

    /**
     * 创建采购订单接口测试所需的供应商。
     */
    private Supplier createSupplier(String scene) {
        Supplier supplier = new Supplier();
        supplier.setSupplierCode("SUP-CTRL-PO-" + scene + "-" + System.nanoTime());
        supplier.setSupplierName("采购接口测试供应商-" + scene);
        supplier.setContactPerson("采购接口测试联系人");
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
        item.setRemark("采购订单明细控制器测试数据");
        return item;
    }

    /**
     * 构造采购到货请求。
     */
    private PurchaseReceiveRequest buildReceiveRequest(PurchaseOrder order, BigDecimal receiveQuantity) {
        PurchaseReceiveRequest request = new PurchaseReceiveRequest();
        request.setWarehouseId(1L);
        request.setCreatedBy(1L);
        request.setRemark("采购到货控制器测试数据");
        PurchaseReceiveRequest.Item item = new PurchaseReceiveRequest.Item();
        item.setPurchaseOrderItemId(order.getItems().get(0).getId());
        item.setReceiveQuantity(receiveQuantity);
        item.setRemark("采购到货明细控制器测试数据");
        request.setItems(Collections.singletonList(item));
        return request;
    }
}
