package com.mouse.mes.service;

import com.mouse.mes.entity.Location;
import com.mouse.mes.entity.Material;
import com.mouse.mes.entity.StockIn;
import com.mouse.mes.entity.StockInItem;
import com.mouse.mes.entity.StockOut;
import com.mouse.mes.entity.StockOutItem;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class StockOutServiceTest {

    /**
     * 出库业务服务，用于测试生产领料出库、销售出库和其他出库。
     */
    @Autowired
    private StockOutService stockOutService;

    /**
     * 入库业务服务，用于先准备可出库库存。
     */
    @Autowired
    private StockInService stockInService;

    /**
     * 库存业务服务，用于验证出库后的库存扣减结果。
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
     * 测试生产领料出库，出库后库存数量应该减少。
     */
    @Test
    public void testProductionIssueStockOutAndInventoryDecrease() {
        TestStockData data = prepareStock("SOUT-PRODUCTION", new BigDecimal("10"));
        StockOut stockOut = buildStockOut("PRODUCTION_ISSUE", data.warehouse.getId(), data.material.getId(), data.location.getId(), new BigDecimal("4"));
        StockOut savedStockOut = stockOutService.create(stockOut);
        BigDecimal availableQuantity = inventoryService.getAvailableQuantity(data.material.getId(), data.warehouse.getId(), data.location.getId(), null);
        System.out.println("生产领料出库结果：" + savedStockOut);
        System.out.println("出库后库存数量：" + availableQuantity);
        assertNotNull(savedStockOut);
        assertEquals(0, availableQuantity.compareTo(new BigDecimal("6")));
    }

    /**
     * 测试销售出库。
     */
    @Test
    public void testSalesStockOut() {
        TestStockData data = prepareStock("SOUT-SALES", new BigDecimal("9"));
        StockOut stockOut = buildStockOut("SALES", data.warehouse.getId(), data.material.getId(), data.location.getId(), new BigDecimal("3"));
        StockOut savedStockOut = stockOutService.create(stockOut);
        System.out.println("销售出库结果：" + savedStockOut);
        assertNotNull(savedStockOut);
        assertEquals("SALES", savedStockOut.getStockOutType());
    }

    /**
     * 测试其他出库。
     */
    @Test
    public void testOtherStockOut() {
        TestStockData data = prepareStock("SOUT-OTHER", new BigDecimal("7"));
        StockOut stockOut = buildStockOut("OTHER", data.warehouse.getId(), data.material.getId(), data.location.getId(), new BigDecimal("2"));
        StockOut savedStockOut = stockOutService.create(stockOut);
        StockOut dbStockOut = stockOutService.findByStockOutNo(savedStockOut.getStockOutNo());
        System.out.println("其他出库按单号查询结果：" + dbStockOut);
        assertNotNull(dbStockOut);
        assertEquals(savedStockOut.getId(), dbStockOut.getId());
    }

    /**
     * 测试库存不足时出库失败，库存数量不应被扣减。
     */
    @Test
    public void testStockOutInsufficientShouldFail() {
        TestStockData data = prepareStock("SOUT-NOT-ENOUGH", new BigDecimal("2"));
        StockOut stockOut = buildStockOut("PRODUCTION_ISSUE", data.warehouse.getId(), data.material.getId(), data.location.getId(), new BigDecimal("9"));
        assertThrows(IllegalArgumentException.class, () -> stockOutService.create(stockOut));
        BigDecimal availableQuantity = inventoryService.getAvailableQuantity(data.material.getId(), data.warehouse.getId(), data.location.getId(), null);
        System.out.println("库存不足出库失败后库存数量：" + availableQuantity);
        assertEquals(0, availableQuantity.compareTo(new BigDecimal("2")));
    }

    /**
     * 测试删除出库单，删除后按ID查询不到。
     */
    @Test
    public void testDeleteStockOut() {
        TestStockData data = prepareStock("SOUT-DELETE", new BigDecimal("5"));
        StockOut stockOut = stockOutService.create(buildStockOut("OTHER", data.warehouse.getId(), data.material.getId(), data.location.getId(), new BigDecimal("1")));
        stockOutService.delete(stockOut.getId());
        StockOut dbStockOut = stockOutService.findById(stockOut.getId());
        System.out.println("删除出库单后查询结果：" + dbStockOut);
        assertNull(dbStockOut);
    }

    /**
     * 测试查询所有出库单时，已删除出库单不能出现。
     */
    @Test
    public void testDeletedStockOutNotInAllList() {
        TestStockData data = prepareStock("SOUT-LIST", new BigDecimal("5"));
        StockOut stockOut = stockOutService.create(buildStockOut("OTHER", data.warehouse.getId(), data.material.getId(), data.location.getId(), new BigDecimal("1")));
        stockOutService.delete(stockOut.getId());
        List<StockOut> stockOutList = stockOutService.findAll();
        boolean exists = stockOutList.stream().anyMatch(item -> item.getId().equals(stockOut.getId()));
        System.out.println("出库单列表数量：" + stockOutList.size());
        assertTrue(!exists);
    }

    /**
     * 准备一个已有库存的测试场景。
     */
    private TestStockData prepareStock(String scene, BigDecimal quantity) {
        Warehouse warehouse = warehouseService.create(buildWarehouse(scene));
        Location location = locationService.create(buildLocation(warehouse.getId(), scene));
        Material material = createMaterial(scene, 5);
        StockIn stockIn = buildStockIn(warehouse.getId(), material.getId(), location.getId(), quantity);
        stockInService.create(stockIn);
        return new TestStockData(warehouse, location, material);
    }

    /**
     * 构造测试仓库。
     */
    private Warehouse buildWarehouse(String scene) {
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseCode("WH-" + scene + "-" + System.nanoTime());
        warehouse.setWarehouseName("测试仓库-" + scene);
        warehouse.setLocation("测试园区E栋");
        warehouse.setManagerId(1L);
        warehouse.setManagerName("仓库管理员");
        warehouse.setStatus(1);
        warehouse.setRemark("出库测试仓库");
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
        material.setRemark("出库测试物料");
        material.setCreateTime(new Date());
        material.setUpdateTime(new Date());
        materialMapper.insert(material);
        return material;
    }

    /**
     * 构造测试入库单，用于准备出库前库存。
     */
    private StockIn buildStockIn(Long warehouseId, Long materialId, Long locationId, BigDecimal quantity) {
        StockIn stockIn = new StockIn();
        stockIn.setStockInNo("SIN-FOR-SOUT-" + System.nanoTime());
        stockIn.setStockInType("PURCHASE");
        stockIn.setWarehouseId(warehouseId);
        stockIn.setStatus("COMPLETED");
        stockIn.setCreatedBy(1L);
        stockIn.setRemark("出库测试前置入库");
        StockInItem item = new StockInItem();
        item.setMaterialId(materialId);
        item.setLocationId(locationId);
        item.setQuantity(quantity);
        item.setRemark("出库测试前置入库明细");
        stockIn.setItems(Collections.singletonList(item));
        return stockIn;
    }

    /**
     * 构造测试出库单。
     */
    private StockOut buildStockOut(String stockOutType, Long warehouseId, Long materialId, Long locationId, BigDecimal quantity) {
        StockOut stockOut = new StockOut();
        stockOut.setStockOutNo("SOUT-" + stockOutType + "-" + System.nanoTime());
        stockOut.setStockOutType(stockOutType);
        stockOut.setWarehouseId(warehouseId);
        stockOut.setSourceOrderNo("SOURCE-" + System.nanoTime());
        stockOut.setStatus("COMPLETED");
        stockOut.setCreatedBy(1L);
        stockOut.setRemark("出库测试单据");
        StockOutItem item = new StockOutItem();
        item.setMaterialId(materialId);
        item.setLocationId(locationId);
        item.setQuantity(quantity);
        item.setRemark("出库测试明细");
        stockOut.setItems(Collections.singletonList(item));
        return stockOut;
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
