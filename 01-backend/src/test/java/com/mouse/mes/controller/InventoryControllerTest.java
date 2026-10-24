package com.mouse.mes.controller;

import com.mouse.mes.entity.Inventory;
import com.mouse.mes.entity.Location;
import com.mouse.mes.entity.Material;
import com.mouse.mes.entity.Warehouse;
import com.mouse.mes.mapper.MaterialMapper;
import com.mouse.mes.service.LocationService;
import com.mouse.mes.service.WarehouseService;
import com.mouse.mes.util.JsonResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InventoryControllerTest {

    /**
     * 库存控制器，用于测试库存接口返回结果。
     */
    @Autowired
    private InventoryController inventoryController;

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
     * 测试新增库存接口。
     */
    @Test
    public void testCreateInventory() {
        TestStockData data = prepareBaseData("CTRL-INV-CREATE", 5);
        JsonResult<Inventory> result = inventoryController.create(buildInventory(data.warehouse.getId(), data.location.getId(), data.material.getId(), new BigDecimal("13")));
        System.out.println("新增库存接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 测试按物料、仓库和库位组合查询库存接口。
     */
    @Test
    public void testSearchInventory() {
        TestStockData data = prepareBaseData("CTRL-INV-SEARCH", 5);
        Inventory inventory = inventoryController.create(buildInventory(data.warehouse.getId(), data.location.getId(), data.material.getId(), new BigDecimal("16"))).getData();
        JsonResult<List<Inventory>> result = inventoryController.search(data.material.getId(), data.warehouse.getId(), data.location.getId());
        boolean exists = result.getData().stream().anyMatch(item -> item.getId().equals(inventory.getId()));
        System.out.println("库存组合查询接口数量：" + result.getData().size());
        assertEquals(200, result.getCode());
        assertTrue(exists);
    }

    /**
     * 测试低库存预警接口。
     */
    @Test
    public void testFindLowStock() {
        TestStockData data = prepareBaseData("CTRL-INV-LOW", 100);
        Inventory inventory = inventoryController.create(buildInventory(data.warehouse.getId(), data.location.getId(), data.material.getId(), new BigDecimal("5"))).getData();
        JsonResult<List<Inventory>> result = inventoryController.findLowStock();
        boolean exists = result.getData().stream().anyMatch(item -> item.getId().equals(inventory.getId()));
        System.out.println("低库存接口数量：" + result.getData().size());
        assertEquals(200, result.getCode());
        assertTrue(exists);
    }

    /**
     * 测试库存记录不存在时查询返回错误。
     */
    @Test
    public void testFindInventoryNotExists() {
        JsonResult<Inventory> result = inventoryController.findById(-999999L);
        System.out.println("查询不存在库存结果：" + result.getMsg());
        assertEquals(500, result.getCode());
    }

    /**
     * 测试删除库存记录接口，删除后再次查询返回错误。
     */
    @Test
    public void testDeleteInventory() {
        TestStockData data = prepareBaseData("CTRL-INV-DELETE", 5);
        Inventory inventory = inventoryController.create(buildInventory(data.warehouse.getId(), data.location.getId(), data.material.getId(), new BigDecimal("7"))).getData();
        JsonResult<Void> deleteResult = inventoryController.delete(inventory.getId());
        JsonResult<Inventory> findResult = inventoryController.findById(inventory.getId());
        System.out.println("删除库存接口结果：" + deleteResult.getMsg());
        assertEquals(200, deleteResult.getCode());
        assertEquals(500, findResult.getCode());
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
        warehouse.setLocation("测试园区H栋");
        warehouse.setManagerId(1L);
        warehouse.setManagerName("仓库管理员");
        warehouse.setStatus(1);
        warehouse.setRemark("库存控制器测试仓库");
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
        material.setRemark("库存控制器测试物料");
        material.setCreateTime(new Date());
        material.setUpdateTime(new Date());
        materialMapper.insert(material);
        return material;
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
