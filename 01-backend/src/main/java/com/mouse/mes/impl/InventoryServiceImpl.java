package com.mouse.mes.impl;

import com.mouse.mes.entity.Inventory;
import com.mouse.mes.entity.InventoryTransaction;
import com.mouse.mes.mapper.InventoryMapper;
import com.mouse.mes.mapper.InventoryTransactionMapper;
import com.mouse.mes.mapper.MaterialMapper;
import com.mouse.mes.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * 库存数量维护与流水记录服务。
 * @author 第26组
 */
@Service
public class InventoryServiceImpl implements InventoryService {

    /**
     * 库存数据库访问对象。
     */
    @Autowired
    private InventoryMapper inventoryMapper;

    /**
     * 库存流水数据库访问对象。
     */
    @Autowired
    private InventoryTransactionMapper inventoryTransactionMapper;

    /**
     * 物料数据库访问对象，用于同步物料总库存。
     */
    @Autowired
    private MaterialMapper materialMapper;

    /**
     * 查询全部库存记录。
     */
    @Override
    public List<Inventory> findAll() {
        return inventoryMapper.findAll();
    }

    /**
     * 按库存ID查询库存记录。
     */
    @Override
    public Inventory findById(Long id) {
        return inventoryMapper.findById(id);
    }

    /**
     * 按仓库ID查询库存。
     */
    @Override
    public List<Inventory> findByWarehouseId(Long warehouseId) {
        return inventoryMapper.findByWarehouseId(warehouseId);
    }

    /**
     * 按物料ID查询库存。
     */
    @Override
    public List<Inventory> findByMaterialId(Long materialId) {
        return inventoryMapper.findByMaterialId(materialId);
    }

    /**
     * 按物料、仓库、库位组合查询库存。
     */
    @Override
    public List<Inventory> search(Long materialId, Long warehouseId, Long locationId) {
        return inventoryMapper.search(materialId, warehouseId, locationId);
    }

    /**
     * 查询库存数量低于物料安全库存的记录。
     */
    @Override
    public List<Inventory> findLowStock() {
        return inventoryMapper.findLowStock();
    }

    /**
     * 查询全部库存流水。
     */
    @Override
    public List<InventoryTransaction> findTransactions() {
        return inventoryTransactionMapper.findAll();
    }

    /**
     * 查询指定物料、仓库、库位、批次下的可用库存数量。
     */
    @Override
    public BigDecimal getAvailableQuantity(Long materialId, Long warehouseId, Long locationId, Long lotId) {
        Inventory inventory = inventoryMapper.findOne(materialId, warehouseId, locationId, lotId);
        return inventory == null || inventory.getQuantity() == null ? BigDecimal.ZERO : inventory.getQuantity();
    }

    /**
     * 入库后增加库存并写入库存流水。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Inventory increaseStock(Long materialId, Long warehouseId, Long locationId, Long lotId, BigDecimal quantity,
                                   String businessType, Long sourceOrderId, String sourceOrderNo, Long createdBy, String remark) {
        validateStockChange(materialId, warehouseId, quantity);
        Inventory inventory = inventoryMapper.findOne(materialId, warehouseId, locationId, lotId);
        BigDecimal beforeQuantity = inventory == null ? BigDecimal.ZERO : safe(inventory.getQuantity());
        BigDecimal afterQuantity = beforeQuantity.add(quantity);
        if (inventory == null) {
            inventory = new Inventory();
            inventory.setWarehouseId(warehouseId);
            inventory.setLocationId(locationId);
            inventory.setMaterialId(materialId);
            inventory.setLotId(lotId);
            inventory.setQuantity(afterQuantity);
            inventory.setLastUpdateTime(new Date());
            inventory.setIsDeleted(0);
            inventoryMapper.insert(inventory);
        } else {
            inventory.setQuantity(afterQuantity);
            inventory.setLastUpdateTime(new Date());
            inventoryMapper.update(inventory);
        }
        saveTransaction("IN", businessType, sourceOrderId, sourceOrderNo, warehouseId, locationId, materialId, lotId,
                quantity, beforeQuantity, afterQuantity, createdBy, remark);
        refreshMaterialStock(materialId);
        return inventoryMapper.findById(inventory.getId());
    }

    /**
     * 出库前校验库存，库存充足时扣减库存并写入库存流水。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Inventory decreaseStock(Long materialId, Long warehouseId, Long locationId, Long lotId, BigDecimal quantity,
                                   String businessType, Long sourceOrderId, String sourceOrderNo, Long createdBy, String remark) {
        validateStockChange(materialId, warehouseId, quantity);
        Inventory inventory = inventoryMapper.findOne(materialId, warehouseId, locationId, lotId);
        BigDecimal beforeQuantity = inventory == null ? BigDecimal.ZERO : safe(inventory.getQuantity());
        if (beforeQuantity.compareTo(quantity) < 0) {
            throw new IllegalArgumentException("库存不足，不能出库");
        }
        BigDecimal afterQuantity = beforeQuantity.subtract(quantity);
        inventory.setQuantity(afterQuantity);
        inventory.setLastUpdateTime(new Date());
        inventoryMapper.update(inventory);
        saveTransaction("OUT", businessType, sourceOrderId, sourceOrderNo, warehouseId, locationId, materialId, lotId,
                quantity, beforeQuantity, afterQuantity, createdBy, remark);
        refreshMaterialStock(materialId);
        return inventoryMapper.findById(inventory.getId());
    }

    /**
     * 手工新增库存记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Inventory create(Inventory inventory) {
        validateInventory(inventory, true);
        if (inventory.getQuantity() == null) {
            inventory.setQuantity(BigDecimal.ZERO);
        }
        inventory.setLastUpdateTime(new Date());
        inventory.setIsDeleted(0);
        inventoryMapper.insert(inventory);
        refreshMaterialStock(inventory.getMaterialId());
        return inventoryMapper.findById(inventory.getId());
    }

    /**
     * 手工修改库存记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Inventory update(Inventory inventory) {
        validateInventory(inventory, false);
        inventory.setLastUpdateTime(new Date());
        inventoryMapper.update(inventory);
        refreshMaterialStock(inventory.getMaterialId());
        return inventoryMapper.findById(inventory.getId());
    }

    /**
     * 逻辑删除库存记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Inventory inventory = inventoryMapper.findById(id);
        if (inventory != null) {
            inventory.setIsDeleted(1);
            inventory.setLastUpdateTime(new Date());
            inventoryMapper.update(inventory);
            refreshMaterialStock(inventory.getMaterialId());
        }
    }

    /**
     * 校验手工维护库存的记录和数量。
     */
    private void validateInventory(Inventory inventory, boolean allowNullQuantity) {
        if (inventory == null) {
            throw new IllegalArgumentException("库存记录不能为空");
        }
        BigDecimal quantity = inventory.getQuantity();
        if (quantity == null) {
            if (!allowNullQuantity) {
                throw new IllegalArgumentException("库存数量不能为空");
            }
        } else if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("库存数量不能为负数");
        }
    }

    /**
     * 校验库存变动基础参数。
     */
    private void validateStockChange(Long materialId, Long warehouseId, BigDecimal quantity) {
        if (materialId == null) {
            throw new IllegalArgumentException("物料ID不能为空");
        }
        if (warehouseId == null) {
            throw new IllegalArgumentException("仓库ID不能为空");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("库存变动数量必须大于0");
        }
    }

    /**
     * 保存库存流水。
     */
    private void saveTransaction(String transactionType, String businessType, Long sourceOrderId, String sourceOrderNo,
                                 Long warehouseId, Long locationId, Long materialId, Long lotId, BigDecimal quantity,
                                 BigDecimal beforeQuantity, BigDecimal afterQuantity, Long createdBy, String remark) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setTransactionNo(nextTransactionNo(transactionType));
        transaction.setTransactionType(transactionType);
        transaction.setBusinessType(businessType);
        transaction.setSourceOrderId(sourceOrderId);
        transaction.setSourceOrderNo(sourceOrderNo);
        transaction.setWarehouseId(warehouseId);
        transaction.setLocationId(locationId);
        transaction.setMaterialId(materialId);
        transaction.setLotId(lotId);
        transaction.setQuantity(quantity);
        transaction.setBeforeQuantity(beforeQuantity);
        transaction.setAfterQuantity(afterQuantity);
        transaction.setCreatedBy(createdBy);
        transaction.setCreatedAt(new Date());
        transaction.setRemark(remark);
        inventoryTransactionMapper.insert(transaction);
    }

    /**
     * 刷新物料主表的总库存，方便物料页面直接查看库存。
     */
    private void refreshMaterialStock(Long materialId) {
        if (materialId == null) {
            return;
        }
        BigDecimal totalQuantity = inventoryMapper.sumQuantityByMaterialId(materialId);
        materialMapper.updateStock(materialId, totalQuantity == null ? 0 : totalQuantity.intValue());
    }

    /**
     * 生成库存流水编号。
     */
    private String nextTransactionNo(String transactionType) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return "INV-" + transactionType + "-" + timestamp + "-" + System.nanoTime();
    }

    /**
     * 将空数量转为零。
     */
    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
