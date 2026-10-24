package com.mouse.mes.impl;

import com.mouse.mes.entity.StockOut;
import com.mouse.mes.entity.StockOutItem;
import com.mouse.mes.mapper.StockOutItemMapper;
import com.mouse.mes.mapper.StockOutMapper;
import com.mouse.mes.mapper.WarehouseMapper;
import com.mouse.mes.service.InventoryService;
import com.mouse.mes.service.StockOutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class StockOutServiceImpl implements StockOutService {
    @Autowired private org.springframework.jdbc.core.JdbcTemplate ledgerDb;

    /**
     * 出库单数据库访问对象。
     */
    @Autowired
    private StockOutMapper stockOutMapper;

    /**
     * 出库单明细数据库访问对象。
     */
    @Autowired
    private StockOutItemMapper stockOutItemMapper;

    /**
     * 仓库数据库访问对象，用于校验仓库是否存在。
     */
    @Autowired
    private WarehouseMapper warehouseMapper;

    /**
     * 库存业务服务，用于出库前校验库存、扣减库存和写流水。
     */
    @Autowired
    private InventoryService inventoryService;

    /**
     * 查询全部未删除出库单，并带出明细。
     */
    @Override
    public List<StockOut> findAll() {
        List<StockOut> stockOuts = stockOutMapper.findAll();
        for (StockOut stockOut : stockOuts) {
            fillItems(stockOut);
        }
        return stockOuts;
    }

    /**
     * 按出库单ID查询出库单，并带出明细。
     */
    @Override
    public StockOut findById(Long id) {
        StockOut stockOut = stockOutMapper.findById(id);
        fillItems(stockOut);
        return stockOut;
    }

    /**
     * 按出库单号查询出库单，并带出明细。
     */
    @Override
    public StockOut findByStockOutNo(String stockOutNo) {
        StockOut stockOut = stockOutMapper.findByStockOutNo(stockOutNo);
        fillItems(stockOut);
        return stockOut;
    }

    /**
     * 创建出库单，先校验库存，库存充足时保存明细、扣减库存并写入库存流水。
     */
    @Override
    @Transactional
    public StockOut create(StockOut stockOut) {
        ledgerDb.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        com.mouse.mes.service.PostingScope.validate(stockOut.getStockOutType(),stockOut.getSourceOrderId());
        validateStockOut(stockOut);
        for (StockOutItem item : stockOut.getItems()) {
            validateStockOutItem(item);
            BigDecimal availableQuantity = inventoryService.getAvailableQuantity(
                    item.getMaterialId(), stockOut.getWarehouseId(), item.getLocationId(), item.getLotId());
            if (availableQuantity.compareTo(item.getQuantity()) < 0) {
                throw new IllegalArgumentException("库存不足，不能出库");
            }
        }
        stockOut.setCreatedAt(new Date());
        stockOut.setUpdatedAt(new Date());
        stockOut.setIsDeleted(0);
        stockOut.setStatus("COMPLETED");
        stockOut.setTotalQuantity(calculateTotalQuantity(stockOut.getItems()));
        stockOutMapper.insert(stockOut);
        for (StockOutItem item : stockOut.getItems()) {
            item.setStockOutId(stockOut.getId());
            stockOutItemMapper.insert(item);
            inventoryService.decreaseStock(item.getMaterialId(), stockOut.getWarehouseId(), item.getLocationId(),
                    item.getLotId(), item.getQuantity(), stockOut.getStockOutType(), stockOut.getId(),
                    stockOut.getStockOutNo(), stockOut.getCreatedBy(), item.getRemark());
        }
        return findById(stockOut.getId());
    }

    /**
     * 修改出库单基础信息，不重新处理库存。
     */
    @Override
    public StockOut update(StockOut stockOut) {
        if(stockOut!=null) throw new IllegalArgumentException("库存已过账，单据不可修改，请保留原始凭据");
        StockOut existing = stockOutMapper.findById(stockOut.getId());
        if (existing == null) {
            throw new IllegalArgumentException("出库单不存在");
        }
        mergeMissingFields(stockOut, existing);
        stockOut.setUpdatedAt(new Date());
        stockOutMapper.update(stockOut);
        return findById(stockOut.getId());
    }

    /**
     * 逻辑删除出库单。
     */
    @Override
    public void delete(Long id) {
        if(id!=null) throw new IllegalArgumentException("库存单据保留追溯记录，不能删除已过账凭据");
        StockOut stockOut = stockOutMapper.findById(id);
        if (stockOut == null) {
            throw new IllegalArgumentException("出库单不存在");
        }
        stockOut.setIsDeleted(1);
        stockOut.setUpdatedAt(new Date());
        stockOutMapper.update(stockOut);
    }

    /**
     * 给出库单填充出库明细。
     */
    private void fillItems(StockOut stockOut) {
        if (stockOut != null) {
            stockOut.setItems(stockOutItemMapper.findByStockOutId(stockOut.getId()));
        }
    }

    /**
     * 校验出库单主表字段。
     */
    private void validateStockOut(StockOut stockOut) {
        if (stockOut == null) {
            throw new IllegalArgumentException("出库单不能为空");
        }
        if (stockOut.getStockOutNo() == null || stockOut.getStockOutNo().isBlank()) {
            throw new IllegalArgumentException("出库单号不能为空");
        }
        if (stockOut.getStockOutType() == null || stockOut.getStockOutType().isBlank()) {
            throw new IllegalArgumentException("出库类型不能为空");
        }
        if (stockOut.getWarehouseId() == null) {
            throw new IllegalArgumentException("仓库ID不能为空");
        }
        if (warehouseMapper.findById(stockOut.getWarehouseId()) == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        if (stockOutMapper.findByStockOutNo(stockOut.getStockOutNo()) != null) {
            throw new IllegalArgumentException("出库单号已存在");
        }
        if (stockOut.getItems() == null || stockOut.getItems().isEmpty()) {
            throw new IllegalArgumentException("出库明细不能为空");
        }
        for(var item:stockOut.getItems()) { validateStockOutItem(item);com.mouse.mes.service.StockPositionValidator.check(ledgerDb,item.getMaterialId(),stockOut.getWarehouseId(),item.getLocationId(),item.getLotId()); }
    }

    /**
     * 校验出库明细字段。
     */
    private void validateStockOutItem(StockOutItem item) {
        if (item == null) {
            throw new IllegalArgumentException("出库明细不能为空");
        }
        if (item.getMaterialId() == null) {
            throw new IllegalArgumentException("物料ID不能为空");
        }
        if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("出库数量必须大于0");
        }
    }

    /**
     * 计算出库总数量。
     */
    private BigDecimal calculateTotalQuantity(List<StockOutItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (StockOutItem item : items) {
            validateStockOutItem(item);
            total = total.add(item.getQuantity());
        }
        return total;
    }

    /**
     * 修改出库单时补齐未传字段，避免被空值覆盖。
     */
    private void mergeMissingFields(StockOut target, StockOut existing) {
        if (target.getStockOutNo() == null || target.getStockOutNo().isBlank()) {
            target.setStockOutNo(existing.getStockOutNo());
        }
        if (target.getStockOutType() == null || target.getStockOutType().isBlank()) {
            target.setStockOutType(existing.getStockOutType());
        }
        if (target.getWarehouseId() == null) {
            target.setWarehouseId(existing.getWarehouseId());
        }
        if (target.getSourceOrderId() == null) {
            target.setSourceOrderId(existing.getSourceOrderId());
        }
        if (target.getSourceOrderNo() == null) {
            target.setSourceOrderNo(existing.getSourceOrderNo());
        }
        if (target.getTotalQuantity() == null) {
            target.setTotalQuantity(existing.getTotalQuantity());
        }
        if (target.getStatus() == null || target.getStatus().isBlank()) {
            target.setStatus(existing.getStatus());
        }
        if (target.getRemark() == null) {
            target.setRemark(existing.getRemark());
        }
        if (target.getCreatedBy() == null) {
            target.setCreatedBy(existing.getCreatedBy());
        }
        target.setCreatedAt(existing.getCreatedAt());
        target.setIsDeleted(existing.getIsDeleted());
    }
}
