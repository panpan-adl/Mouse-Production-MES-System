package com.mouse.mes.service;

import com.mouse.mes.entity.Supplier;

import java.util.List;

public interface SupplierService {
    /**
     * 查询全部未删除供应商，活跃供应商排在禁用供应商前面。
     */
    List<Supplier> findAll();

    /**
     * 按供应商ID查询未删除供应商。
     */
    Supplier findById(Long id);

    /**
     * 按供应商编码查询未删除供应商。
     */
    Supplier findBySupplierCode(String supplierCode);

    /**
     * 按供应商编码、名称和状态组合查询供应商；状态为空时查询全部未删除供应商。
     */
    List<Supplier> search(String supplierCode, String supplierName, Integer status);

    /**
     * 新增供应商。
     */
    Supplier create(Supplier supplier);

    /**
     * 修改供应商。
     */
    Supplier update(Supplier supplier);

    /**
     * 删除供应商，删除后不再出现在普通查询中。
     */
    void delete(Long id);

    /**
     * 禁用供应商。
     */
    Supplier disable(Long id);

    /**
     * 启用供应商，用于恢复被禁用的供应商。
     */
    Supplier enable(Long id);
}
