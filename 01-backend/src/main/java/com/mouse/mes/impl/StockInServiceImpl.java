package com.mouse.mes.impl;

import com.mouse.mes.entity.StockIn;
import com.mouse.mes.entity.StockInItem;
import com.mouse.mes.mapper.StockInItemMapper;
import com.mouse.mes.mapper.StockInMapper;
import com.mouse.mes.mapper.WarehouseMapper;
import com.mouse.mes.service.InventoryService;
import com.mouse.mes.service.StockInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class StockInServiceImpl implements StockInService {
    @Autowired private org.springframework.jdbc.core.JdbcTemplate ledgerDb;

    /**
     * 入库单数据库访问对象。
     */
    @Autowired
    private StockInMapper stockInMapper;

    /**
     * 入库单明细数据库访问对象。
     */
    @Autowired
    private StockInItemMapper stockInItemMapper;

    /**
     * 仓库数据库访问对象，用于校验仓库是否存在。
     */
    @Autowired
    private WarehouseMapper warehouseMapper;

    /**
     * 库存业务服务，用于入库后增加库存和写流水。
     */
    @Autowired
    private InventoryService inventoryService;

    /**
     * 查询全部未删除入库单，并带出明细。
     */
    @Override
    public List<StockIn> findAll() {
        List<StockIn> stockIns = stockInMapper.findAll();
        for (StockIn stockIn : stockIns) {
            fillItems(stockIn);
        }
        return stockIns;
    }

    /**
     * 按入库单ID查询入库单，并带出明细。
     */
    @Override
    public StockIn findById(Long id) {
        StockIn stockIn = stockInMapper.findById(id);
        fillItems(stockIn);
        return stockIn;
    }

    /**
     * 按入库单号查询入库单，并带出明细。
     */
    @Override
    public StockIn findByStockInNo(String stockInNo) {
        StockIn stockIn = stockInMapper.findByStockInNo(stockInNo);
        fillItems(stockIn);
        return stockIn;
    }

    /**
     * 创建入库单，保存明细，增加库存并写入库存流水。
     */
    @Override
    @Transactional
    public StockIn create(StockIn stockIn) {
        ledgerDb.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        com.mouse.mes.service.PostingScope.validate(stockIn.getStockInType(),stockIn.getSourceOrderId());
        validateStockIn(stockIn);
        stockIn.setCreatedAt(new Date());
        stockIn.setUpdatedAt(new Date());
        stockIn.setIsDeleted(0);
        stockIn.setStatus("COMPLETED");
        stockIn.setTotalQuantity(calculateTotalQuantity(stockIn.getItems()));
        stockInMapper.insert(stockIn);
        for (StockInItem item : stockIn.getItems()) {
            validateStockInItem(item);
            item.setStockInId(stockIn.getId());
            stockInItemMapper.insert(item);
            inventoryService.increaseStock(item.getMaterialId(), stockIn.getWarehouseId(), item.getLocationId(),
                    item.getLotId(), item.getQuantity(), stockIn.getStockInType(), stockIn.getId(),
                    stockIn.getStockInNo(), stockIn.getCreatedBy(), item.getRemark());
        }
        return findById(stockIn.getId());
    }

    /**
     * 修改入库单基础信息，不重新处理库存。
     */
    @Override
    public StockIn update(StockIn stockIn) {
        if(stockIn!=null) throw new IllegalArgumentException("库存已过账，单据不可修改，请保留原始凭据");
        StockIn existing = stockInMapper.findById(stockIn.getId());
        if (existing == null) {
            throw new IllegalArgumentException("入库单不存在");
        }
        mergeMissingFields(stockIn, existing);
        stockIn.setUpdatedAt(new Date());
        stockInMapper.update(stockIn);
        return findById(stockIn.getId());
    }

    /**
     * 逻辑删除入库单。
     */
    @Override
    public void delete(Long id) {
        if(id!=null) throw new IllegalArgumentException("库存单据保留追溯记录，不能删除已过账凭据");
        StockIn stockIn = stockInMapper.findById(id);
        if (stockIn == null) {
            throw new IllegalArgumentException("入库单不存在");
        }
        stockIn.setIsDeleted(1);
        stockIn.setUpdatedAt(new Date());
        stockInMapper.update(stockIn);
    }

    /**
     * 给入库单填充入库明细。
     */
    private void fillItems(StockIn stockIn) {
        if (stockIn != null) {
            stockIn.setItems(stockInItemMapper.findByStockInId(stockIn.getId()));
        }
    }

    /**
     * 校验入库单主表字段。
     */
    private void validateStockIn(StockIn stockIn) {
        if (stockIn == null) {
            throw new IllegalArgumentException("入库单不能为空");
        }
        if (stockIn.getStockInNo() == null || stockIn.getStockInNo().isBlank()) {
            throw new IllegalArgumentException("入库单号不能为空");
        }
        if (stockIn.getStockInType() == null || stockIn.getStockInType().isBlank()) {
            throw new IllegalArgumentException("入库类型不能为空");
        }
        if (stockIn.getWarehouseId() == null) {
            throw new IllegalArgumentException("仓库ID不能为空");
        }
        if (warehouseMapper.findById(stockIn.getWarehouseId()) == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        if (stockInMapper.findByStockInNo(stockIn.getStockInNo()) != null) {
            throw new IllegalArgumentException("入库单号已存在");
        }
        if (stockIn.getItems() == null || stockIn.getItems().isEmpty()) {
            throw new IllegalArgumentException("入库明细不能为空");
        }
        for(var item:stockIn.getItems()) { validateStockInItem(item);com.mouse.mes.service.StockPositionValidator.check(ledgerDb,item.getMaterialId(),stockIn.getWarehouseId(),item.getLocationId(),item.getLotId()); }
    }

    /**
     * 校验入库明细字段。
     */
    private void validateStockInItem(StockInItem item) {
        if (item == null) {
            throw new IllegalArgumentException("入库明细不能为空");
        }
        if (item.getMaterialId() == null) {
            throw new IllegalArgumentException("物料ID不能为空");
        }
        if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("入库数量必须大于0");
        }
    }

    /**
     * 计算入库总数量。
     */
    private BigDecimal calculateTotalQuantity(List<StockInItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (StockInItem item : items) {
            validateStockInItem(item);
            total = total.add(item.getQuantity());
        }
        return total;
    }

    /**
     * 修改入库单时补齐未传字段，避免被空值覆盖。
     */
    private void mergeMissingFields(StockIn target, StockIn existing) {
        if (target.getStockInNo() == null || target.getStockInNo().isBlank()) {
            target.setStockInNo(existing.getStockInNo());
        }
        if (target.getStockInType() == null || target.getStockInType().isBlank()) {
            target.setStockInType(existing.getStockInType());
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
