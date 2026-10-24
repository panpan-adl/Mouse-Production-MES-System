package com.mouse.mes.mapper;

import com.mouse.mes.entity.PurchaseOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PurchaseOrderItemMapper {
    /**
     * 按采购订单ID查询订单下所有采购明细。
     */
    List<PurchaseOrderItem> findByPoId(@Param("poId") Long poId);

    /**
     * 按采购明细ID查询单行采购明细。
     */
    PurchaseOrderItem findById(@Param("id") Long id);

    /**
     * 新增采购订单明细。
     */
    int insert(PurchaseOrderItem purchaseOrderItem);

    /**
     * 更新采购明细的累计到货数量。
     */
    int updateReceivedQuantity(@Param("id") Long id, @Param("receivedQuantity") java.math.BigDecimal receivedQuantity);

    /**
     * 删除某个采购订单下的全部明细，用于修改订单时整体替换明细。
     */
    int deleteByPoId(@Param("poId") Long poId);
}
