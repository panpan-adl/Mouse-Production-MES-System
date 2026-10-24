package com.mouse.mes.service;

import com.mouse.mes.entity.Inventory;
import com.mouse.mes.entity.InventoryTransaction;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class InventoryServiceTest {

    /**
     * 库存业务服务，用于测试库存查询、库存增减、库存流水和低库存预警。
     */
    @Autowired
    private InventoryService inventoryService;

    /**
     * 入库业务服务，用于生成库存流水。
     */
    @Autowired
    private StockInService stockInService;

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
     * 测试按物料、仓库和库位组合查询库存。
     */
    @Test
    public void testSearchInventoryByMaterialWarehouseLocation() {
        TestStockData data = prepareBaseData("INV-SEARCH", 5);
        Inventory inventory = inventoryService.create(buildInventory(data.warehouse.getId(), data.location.getId(), data.material.getId(), new BigDecimal("15")));
        List<Inventory> inventoryList = inventoryService.search(data.material.getId(), data.warehouse.getId(), data.location.getId());
        boolean exists = inventoryList.stream().anyMatch(item -> item.getId().equals(inventory.getId()));
        System.out.println("组合查询库存数量：" + inventoryList.size());
        assertTrue(exists);
    }

    /**
     * 测试手工新增库存后，按ID能查询到。
     */
    @Test
    public void testCreateInventory() {
        TestStockData data = prepareBaseData("INV-CREATE", 5);
        Inventory inventory = inventoryService.create(buildInventory(data.warehouse.getId(), data.location.getId(), data.material.getId(), new BigDecimal("11")));
        Inventory dbInventory = inventoryService.findById(inventory.getId());
        System.out.println("新增库存结果：" + dbInventory);
        assertNotNull(dbInventory);
        assertEquals(0, dbInventory.getQuantity().compareTo(new BigDecimal("11")));
    }

    /**
     * 测试入库后会生成库存流水记录。
     */
    @Test
    public void testInventoryTransactionRecordedAfterStockIn() {
        TestStockData data = prepareBaseData("INV-TRANSACTION", 5);
        StockIn stockIn = buildStockIn(data.warehouse.getId(), data.material.getId(), data.location.getId(), new BigDecimal("9"));
        StockIn savedStockIn = stockInService.create(stockIn);
        List<InventoryTransaction> transactionList = inventoryService.findTransactions();
        boolean exists = transactionList.stream().anyMatch(item -> savedStockIn.getStockInNo().equals(item.getSourceOrderNo()));
        System.out.println("库存流水数量：" + transactionList.size());
        assertTrue(exists);
    }

    /**
     * 测试低库存物料能被查询出来。
     */
    @Test
    public void testLowStockInventoryCanBeFound() {
        TestStockData data = prepareBaseData("INV-LOW", 100);
        Inventory inventory = inventoryService.create(buildInventory(data.warehouse.getId(), data.location.getId(), data.material.getId(), new BigDecimal("5")));
        List<Inventory> lowStockList = inventoryService.findLowStock();
        boolean exists = lowStockList.stream().anyMatch(item -> item.getId().equals(inventory.getId()));
        System.out.println("低库存记录数量：" + lowStockList.size());
        assertTrue(exists);
    }

    /**
     * 测试出库库存不足时直接失败。
     */
    @Test
    public void testDecreaseStockInsufficientShouldFail() {
        TestStockData data = prepareBaseData("INV-NOT-ENOUGH", 5);
        inventoryService.create(buildInventory(data.warehouse.getId(), data.location.getId(), data.material.getId(), new BigDecimal("2")));
        try {
            inventoryService.decreaseStock(data.material.getId(), data.warehouse.getId(), data.location.getId(), null,
                    new BigDecimal("6"), "OTHER", null, "TEST-NOT-ENOUGH", 1L, "库存不足测试");
        } catch (IllegalArgumentException exception) {
            System.out.println("库存不足扣减结果：" + exception.getMessage());
            assertEquals("库存不足，不能出库", exception.getMessage());
            return;
        }
        throw new AssertionError("库存不足时应该抛出异常");
    }

    /**
     * 准备仓库、库位和物料基础数据。
     */
    private TestStockData prepareBaseData(String scene, Integer safetyStock) {
        Warehouse warehouse = warehouseService.create(buildWarehouse(scene));
        Location location = locationService.create(buildLocation(warehouse.getId(), scene));
        Material material = createMaterial(scene, safetyStock);
        return new TestStockData(warehouse, location, material);
    }

    /**
     * 构造测试库存记录。
     */
    private Inventory buildInventory(Long warehouseId, Long locationId, Long materialId, BigDecimal quantity) {
        Inventory inventory = new Inventory();
        inventory.setWarehouseId(warehouseId);
        inventory.setLocationId(locationId);
        inventory.setMaterialId(materialId);
        inventory.setQuantity(quantity);
        return inventory;
    }

    /**
     * 构造测试仓库。
     */
    private Warehouse buildWarehouse(String scene) {
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseCode("WH-" + scene + "-" + System.nanoTime());
        warehouse.setWarehouseName("测试仓库-" + scene);
        warehouse.setLocation("测试园区G栋");
        warehouse.setManagerId(1L);
        warehouse.setManagerName("仓库管理员");
        warehouse.setStatus(1);
        warehouse.setRemark("库存测试仓库");
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
        material.setRemark("库存测试物料");
        material.setCreateTime(new Date());
        material.setUpdateTime(new Date());
        materialMapper.insert(material);
        return material;
    }

    /**
     * 构造测试入库单，用于生成库存流水。
     */
    private StockIn buildStockIn(Long warehouseId, Long materialId, Long locationId, BigDecimal quantity) {
        StockIn stockIn = new StockIn();
        stockIn.setStockInNo("SIN-FOR-INV-" + System.nanoTime());
        stockIn.setStockInType("PURCHASE");
        stockIn.setWarehouseId(warehouseId);
        stockIn.setStatus("COMPLETED");
        stockIn.setCreatedBy(1L);
        stockIn.setRemark("库存流水测试入库");
        StockInItem item = new StockInItem();
        item.setMaterialId(materialId);
        item.setLocationId(locationId);
        item.setQuantity(quantity);
        item.setRemark("库存流水测试明细");
        stockIn.setItems(Collections.singletonList(item));
        return stockIn;
    }

    /**
     * 测试库存场景数据，保存仓库、库位和物料。
     */
    private static class TestStockData {

        /**
         * 测试仓库。
         */
        private final Warehouse warehouse;

        /**
         * 测试库位。
         */
        private final Location location;

        /**
         * 测试物料。
         */
        private final Material material;

        private TestStockData(Warehouse warehouse, Location location, Material material) {
            this.warehouse = warehouse;
            this.location = location;
            this.material = material;
        }
    }
}
