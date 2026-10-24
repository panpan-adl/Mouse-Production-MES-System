package com.mouse.mes.mapper;

import com.mouse.mes.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InventoryMapper {
    /**
     * 查询全部未删除库存记录。
     */
    List<Inventory> findAll();

    /**
     * 按库存ID查询库存记录。
     */
    Inventory findById(@Param("id") Long id);

    /**
     * 按仓库ID查询库存。
     */
    List<Inventory> findByWarehouseId(@Param("warehouseId") Long warehouseId);

    /**
     * 按物料ID查询库存。
     */
    List<Inventory> findByMaterialId(@Param("materialId") Long materialId);

    /**
     * 按物料、仓库、库位组合查询库存；参数为空时不作为过滤条件。
     */
    List<Inventory> search(@Param("materialId") Long materialId,
                           @Param("warehouseId") Long warehouseId,
                           @Param("locationId") Long locationId);

    /**
     * 查询某个物料在指定仓库、库位、批次下的库存记录。
     */
    Inventory findOne(@Param("materialId") Long materialId,
                      @Param("warehouseId") Long warehouseId,
                      @Param("locationId") Long locationId,
                      @Param("lotId") Long lotId);

    /**
     * 查询低库存记录：库存数量小于物料安全库存。
     */
    List<Inventory> findLowStock();

    /**
     * 汇总某个物料在所有仓库的库存数量。
     */
    java.math.BigDecimal sumQuantityByMaterialId(@Param("materialId") Long materialId);

    /**
     * 新增库存记录。
     */
    int insert(Inventory inventory);

    /**
     * 修改库存记录。
     */
    int update(Inventory inventory);

    /**
     * 逻辑删除库存记录。
     */
    int delete(@Param("id") Long id);
}
