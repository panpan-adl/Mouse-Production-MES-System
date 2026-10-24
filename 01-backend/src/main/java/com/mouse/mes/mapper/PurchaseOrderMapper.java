package com.mouse.mes.mapper;

import com.mouse.mes.entity.PurchaseOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PurchaseOrderMapper {
    /**
     * 查询全部未删除采购订单。
     */
    List<PurchaseOrder> findAll();

    /**
     * 按采购订单状态查询未删除采购订单。
     */
    List<PurchaseOrder> findByStatus(@Param("status") String status);

    /**
     * 按采购订单ID查询未删除采购订单。
     */
    PurchaseOrder findById(@Param("id") Long id);

    /**
     * 按采购订单编号查询未删除采购订单。
     */
    PurchaseOrder findByPoNo(@Param("poNo") String poNo);

    /**
     * 按采购订单编号查询（包含已删除的）。
     */
    PurchaseOrder findByPoNoIncludingDeleted(@Param("poNo") String poNo);

    /**
     * 新增采购订单。
     */
    int insert(PurchaseOrder purchaseOrder);

    /**
     * 修改采购订单。
     */
    int update(PurchaseOrder purchaseOrder);

    /**
     * 逻辑删除采购订单。
     */
    int delete(@Param("id") Long id);
}
