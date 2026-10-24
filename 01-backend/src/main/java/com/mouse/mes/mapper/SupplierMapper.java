package com.mouse.mes.mapper;

import com.mouse.mes.entity.Supplier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SupplierMapper {
    /**
     * 查询全部未删除供应商，活跃供应商排在禁用供应商前面。
     */
    List<Supplier> findAll();

    /**
     * 按供应商ID查询未删除供应商。
     */
    Supplier findById(@Param("id") Long id);

    /**
     * 按供应商编码查询未删除供应商。
     */
    Supplier findBySupplierCode(@Param("supplierCode") String supplierCode);

    /**
     * 按供应商编码、名称和状态组合查询供应商；状态为空时查询全部未删除供应商。
     */
    List<Supplier> search(@Param("supplierCode") String supplierCode,
                          @Param("supplierName") String supplierName,
                          @Param("status") Integer status);

    /**
     * 新增供应商。
     */
    int insert(Supplier supplier);

    /**
     * 修改供应商。
     */
    int update(Supplier supplier);

    /**
     * 删除供应商，删除后不再出现在普通查询中。
     */
    int delete(@Param("id") Long id);

    /**
     * 禁用供应商，禁用后不能再创建采购订单，但仍可查询和恢复。
     */
    int disable(@Param("id") Long id);

    /**
     * 启用供应商，用于恢复被禁用的供应商。
     */
    int enable(@Param("id") Long id);
}
