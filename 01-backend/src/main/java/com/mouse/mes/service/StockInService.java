package com.mouse.mes.service;

import com.mouse.mes.entity.StockIn;

import java.util.List;

public interface StockInService {

    /**
     * 查询全部未删除入库单。
     */
    List<StockIn> findAll();

    /**
     * 按入库单ID查询入库单。
     */
    StockIn findById(Long id);

    /**
     * 按入库单号查询入库单。
     */
    StockIn findByStockInNo(String stockInNo);

    /**
     * 创建入库单，保存入库明细，增加库存并写入库存流水。
     */
    StockIn create(StockIn stockIn);

    /**
     * 修改入库单基础信息，不重新处理库存。
     */
    StockIn update(StockIn stockIn);

    /**
     * 逻辑删除入库单。
     */
    void delete(Long id);
}
