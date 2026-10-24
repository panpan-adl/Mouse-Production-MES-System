package com.mouse.mes.service;

import com.mouse.mes.dto.PurchaseOrderRequest;
import com.mouse.mes.dto.PurchaseReceiveRequest;
import com.mouse.mes.entity.PurchaseOrder;

import java.util.List;

public interface PurchaseOrderService {
    /**
     * 查询全部未删除采购订单。
     */
    List<PurchaseOrder> findAll();

    /**
     * 按采购状态查询采购订单。
     */
    List<PurchaseOrder> findByStatus(String status);

    /**
     * 按采购订单ID查询采购订单。
     */
    PurchaseOrder findById(Long id);

    /**
     * 按采购订单编号查询采购订单。
     */
    PurchaseOrder findByPoNo(String poNo);

    /**
     * 兼容旧接口的采购订单新增方法。
     */
    PurchaseOrder create(PurchaseOrder purchaseOrder);

    /**
     * 新增采购订单，并保存多个采购订单明细。
     */
    PurchaseOrder create(PurchaseOrderRequest request);

    /**
     * 兼容旧接口的采购订单修改方法。
     */
    PurchaseOrder update(PurchaseOrder purchaseOrder);

    /**
     * 修改采购订单，并可整体替换采购订单明细。
     */
    PurchaseOrder update(Long id, PurchaseOrderRequest request);

    /**
     * 将采购订单从草稿提交为已提交。
     */
    PurchaseOrder submit(Long id);

    /**
     * 将采购订单从已提交审核为已审核。
     */
    PurchaseOrder approve(Long id);

    /**
     * 取消采购订单。
     */
    PurchaseOrder cancel(Long id);

    /**
     * 采购到货，生成入库单并更新采购订单状态。
     */
    PurchaseOrder receive(Long id, PurchaseReceiveRequest request);

    /**
     * 逻辑删除采购订单。
     */
    void delete(Long id);

    /**
     * 接受销售订单并自动创建采购单。
     */
    PurchaseOrder createFromSalesOrder(Long salesOrderId);

    /**
     * 接受销售订单并自动创建采购单（带供应商和预计到货日期）。
     */
    PurchaseOrder createFromSalesOrder(Long salesOrderId, Long supplierId, String expectedDate);
}
