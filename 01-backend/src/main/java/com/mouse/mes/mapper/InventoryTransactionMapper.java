package com.mouse.mes.mapper;

import com.mouse.mes.entity.InventoryTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InventoryTransactionMapper {

    /**
     * 查询全部库存流水。
     */
    List<InventoryTransaction> findAll();

    /**
     * 按库存流水ID查询库存流水。
     */
    InventoryTransaction findById(@Param("id") Long id);

    /**
     * 按物料ID查询库存流水。
     */
    List<InventoryTransaction> findByMaterialId(@Param("materialId") Long materialId);

    /**
     * 按来源单据查询库存流水。
     */
    List<InventoryTransaction> findBySourceOrder(@Param("sourceOrderNo") String sourceOrderNo);

    /**
     * 新增库存流水。
     */
    int insert(InventoryTransaction transaction);
}
