package com.mouse.mes.service;

import com.mouse.mes.entity.Inventory;
import com.mouse.mes.entity.InventoryTransaction;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryService {
    /**
     * 查询全部库存记录。
     */
    List<Inventory> findAll();

    /**
     * 按库存ID查询库存记录。
     */
    Inventory findById(Long id);

    /**
     * 按仓库ID查询库存。
     */
    List<Inventory> findByWarehouseId(Long warehouseId);

    /**
     * 按物料ID查询库存。
     */
    List<Inventory> findByMaterialId(Long materialId);

    /**
     * 按物料、仓库、库位组合查询库存。
     */
    List<Inventory> search(Long materialId, Long warehouseId, Long locationId);

    /**
     * 查询库存数量低于安全库存的记录。
     */
    List<Inventory> findLowStock();

    /**
     * 查询库存流水。
     */
    List<InventoryTransaction> findTransactions();

    /**
     * 查询指定维度的可用库存数量。
     */
    BigDecimal getAvailableQuantity(Long materialId, Long warehouseId, Long locationId, Long lotId);

    /**
     * 入库后增加库存并写入库存流水。
     */
    Inventory increaseStock(Long materialId, Long warehouseId, Long locationId, Long lotId, BigDecimal quantity,
                            String businessType, Long sourceOrderId, String sourceOrderNo, Long createdBy, String remark);

    /**
     * 出库前校验库存，库存充足时扣减库存并写入库存流水。
     */
    Inventory decreaseStock(Long materialId, Long warehouseId, Long locationId, Long lotId, BigDecimal quantity,
                            String businessType, Long sourceOrderId, String sourceOrderNo, Long createdBy, String remark);

    /**
     * 手工新增库存记录。
     */
    Inventory create(Inventory inventory);

    /**
     * 手工修改库存记录。
     */
    Inventory update(Inventory inventory);

    /**
     * 逻辑删除库存记录。
     */
    void delete(Long id);
}
