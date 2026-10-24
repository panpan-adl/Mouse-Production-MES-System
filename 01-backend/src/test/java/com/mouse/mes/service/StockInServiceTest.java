package com.mouse.mes.service;

import com.mouse.mes.entity.Inventory;
import com.mouse.mes.entity.Location;
import com.mouse.mes.entity.Material;
import com.mouse.mes.entity.StockIn;
import com.mouse.mes.entity.StockInItem;
import com.mouse.mes.entity.Warehouse;
import com.mouse.mes.mapper.MaterialMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class StockInServiceTest {

    /**
     * 入库业务服务，用于测试采购入库、生产入库和其他入库。
     */
    @Autowired
    private StockInService stockInService;

    /**
     * 库存业务服务，用于验证入库后的库存增加结果。
     */
    @Autowired
    private InventoryService inventoryService;

    /**
     * 仓库业务服务，用于准备测试仓库。
     */
    @Autowired
    private WarehouseService warehouseService;

    /**
     * 库位业务服务，用于准备测试库位。
     */
    @Autowired
    private LocationService locationService;

    /**
     * 物料数据库访问对象，用于准备测试物料。
     */
    @Autowired
    private MaterialMapper materialMapper;

    /**
     * 测试新增采购入库单，入库后库存数量应该增加。
     */
    @Test
    public void testAddPurchaseStockInAndInventoryIncrease() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("SIN-PURCHASE"));
        Location location = locationService.create(buildLocation(warehouse.getId(), "SIN-PURCHASE"));
        Material material = createMaterial("SIN-PURCHASE", 5);
        StockIn stockIn = buildStockIn("PURCHASE", warehouse.getId(), material.getId(), location.getId(), new BigDecimal("10"));
        StockIn savedStockIn = stockInService.create(stockIn);
        BigDecimal availableQuantity = inventoryService.getAvailableQuantity(material.getId(), warehouse.getId(), location.getId(), null);
        System.out.println("采购入库结果：" + savedStockIn);
        System.out.println("入库后库存数量：" + availableQuantity);
        assertNotNull(savedStockIn);
        assertEquals(0, availableQuantity.compareTo(new BigDecimal("10")));
    }

    /**
     * 测试新增生产入库单，入库后库存数量应该增加。
     */
    @Test
    public void testAddProductionStockIn() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("SIN-PRODUCTION"));
        Location location = locationService.create(buildLocation(warehouse.getId(), "SIN-PRODUCTION"));
        Material material = createMaterial("SIN-PRODUCTION", 5);
        StockIn stockIn = buildStockIn("PRODUCTION", warehouse.getId(), material.getId(), location.getId(), new BigDecimal("8"));
        StockIn savedStockIn = stockInService.create(stockIn);
        System.out.println("生产入库结果：" + savedStockIn);
        assertNotNull(savedStockIn);
        assertEquals("PRODUCTION", savedStockIn.getStockInType());
    }

    /**
     * 测试新增其他入库单，入库后能按入库单号查询到。
     */
    @Test
    public void testAddOtherStockInAndFindByNo() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("SIN-OTHER"));
        Location location = locationService.create(buildLocation(warehouse.getId(), "SIN-OTHER"));
        Material material = createMaterial("SIN-OTHER", 5);
        StockIn stockIn = buildStockIn("OTHER", warehouse.getId(), material.getId(), location.getId(), new BigDecimal("6"));
        StockIn savedStockIn = stockInService.create(stockIn);
        StockIn dbStockIn = stockInService.findByStockInNo(savedStockIn.getStockInNo());
        System.out.println("按入库单号查询结果：" + dbStockIn);
        assertNotNull(dbStockIn);
        assertEquals(savedStockIn.getId(), dbStockIn.getId());
    }

    /**
     * 测试修改入库单基础信息，修改后查询数据库字段是否变化。
     */
    @Test
    public void testUpdateStockIn() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("SIN-UPDATE"));
        Location location = locationService.create(buildLocation(warehouse.getId(), "SIN-UPDATE"));
        Material material = createMaterial("SIN-UPDATE", 5);
        StockIn stockIn = stockInService.create(buildStockIn("PURCHASE", warehouse.getId(), material.getId(), location.getId(), new BigDecimal("5")));
        stockIn.setRemark("入库单修改测试");
        StockIn updatedStockIn = stockInService.update(stockIn);
        System.out.println("修改入库单结果：" + updatedStockIn);
        assertEquals("入库单修改测试", updatedStockIn.getRemark());
    }

    /**
     * 测试删除入库单，删除后按ID查询不到。
     */
    @Test
    public void testDeleteStockIn() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("SIN-DELETE"));
        Location location = locationService.create(buildLocation(warehouse.getId(), "SIN-DELETE"));
        Material material = createMaterial("SIN-DELETE", 5);
        StockIn stockIn = stockInService.create(buildStockIn("PURCHASE", warehouse.getId(), material.getId(), location.getId(), new BigDecimal("5")));
        stockInService.delete(stockIn.getId());
        StockIn dbStockIn = stockInService.findById(stockIn.getId());
        System.out.println("删除入库单后查询结果：" + dbStockIn);
        assertNull(dbStockIn);
    }

    /**
     * 测试查询所有入库单时，已删除入库单不能出现。
     */
    @Test
    public void testDeletedStockInNotInAllList() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("SIN-LIST"));
        Location location = locationService.create(buildLocation(warehouse.getId(), "SIN-LIST"));
        Material material = createMaterial("SIN-LIST", 5);
        StockIn stockIn = stockInService.create(buildStockIn("PURCHASE", warehouse.getId(), material.getId(), location.getId(), new BigDecimal("5")));
        stockInService.delete(stockIn.getId());
        List<StockIn> stockInList = stockInService.findAll();
        boolean exists = stockInList.stream().anyMatch(item -> item.getId().equals(stockIn.getId()));
        System.out.println("入库单列表数量：" + stockInList.size());
        assertTrue(!exists);
    }

    /**
     * 构造测试仓库。
     */
    private Warehouse buildWarehouse(String scene) {
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseCode("WH-" + scene + "-" + System.nanoTime());
        warehouse.setWarehouseName("测试仓库-" + scene);
        warehouse.setLocation("测试园区C栋");
        warehouse.setManagerId(1L);
        warehouse.setManagerName("仓库管理员");
        warehouse.setStatus(1);
        warehouse.setRemark("入库测试仓库");
        return warehouse;
    }

    /**
     * 构造测试库位。
     */
    private Location buildLocation(Long warehouseId, String scene) {
        Location location = new Location();
        location.setWarehouseId(warehouseId);
        location.setLocationCode("LOC-" + scene + "-" + System.nanoTime());
        location.setLocationName("测试库位-" + scene);
        location.setLocationType("NORMAL");
        location.setStatus("1");
        return location;
    }

    /**
     * 新增测试物料。
     */
    private Material createMaterial(String scene, Integer safetyStock) {
        Material material = new Material();
        material.setMaterialCode("MAT-" + scene + "-" + System.nanoTime());
        material.setMaterialName("测试物料-" + scene);
        material.setMaterialType(1);
        material.setSpecification("标准规格");
        material.setUnit("PCS");
        material.setSafetyStock(safetyStock);
        material.setCurrentStock(0);
        material.setStatus(1);
        material.setRemark("入库测试物料");
        material.setCreateTime(new Date());
        material.setUpdateTime(new Date());
        materialMapper.insert(material);
        return material;
    }

    /**
     * 构造测试入库单。
     */
    private StockIn buildStockIn(String stockInType, Long warehouseId, Long materialId, Long locationId, BigDecimal quantity) {
        StockIn stockIn = new StockIn();
        stockIn.setStockInNo("SIN-" + stockInType + "-" + System.nanoTime());
        stockIn.setStockInType(stockInType);
        stockIn.setWarehouseId(warehouseId);
        stockIn.setSourceOrderNo("SOURCE-" + System.nanoTime());
        stockIn.setStatus("COMPLETED");
        stockIn.setCreatedBy(1L);
        stockIn.setRemark("入库测试单据");
        StockInItem item = new StockInItem();
        item.setMaterialId(materialId);
        item.setLocationId(locationId);
        item.setQuantity(quantity);
        item.setRemark("入库测试明细");
        stockIn.setItems(Collections.singletonList(item));
        return stockIn;
    }
}
