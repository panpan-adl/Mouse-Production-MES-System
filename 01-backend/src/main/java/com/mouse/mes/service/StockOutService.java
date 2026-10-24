package com.mouse.mes.service;

import com.mouse.mes.entity.StockOut;

import java.util.List;

public interface StockOutService {

    /**
     * 查询全部未删除出库单。
     */
    List<StockOut> findAll();

    /**
     * 按出库单ID查询出库单。
     */
    StockOut findById(Long id);

    /**
     * 按出库单号查询出库单。
     */
    StockOut findByStockOutNo(String stockOutNo);

    /**
     * 创建出库单，校验库存，保存出库明细，扣减库存并写入库存流水。
     */
    StockOut create(StockOut stockOut);

    /**
     * 修改出库单基础信息，不重新处理库存。
     */
    StockOut update(StockOut stockOut);

    /**
     * 逻辑删除出库单。
     */
    void delete(Long id);
}
