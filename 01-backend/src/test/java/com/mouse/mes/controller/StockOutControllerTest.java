package com.mouse.mes.controller;

import com.mouse.mes.entity.Location;
import com.mouse.mes.entity.Material;
import com.mouse.mes.entity.StockIn;
import com.mouse.mes.entity.StockInItem;
import com.mouse.mes.entity.StockOut;
import com.mouse.mes.entity.StockOutItem;
import com.mouse.mes.entity.Warehouse;
import com.mouse.mes.mapper.MaterialMapper;
import com.mouse.mes.service.LocationService;
import com.mouse.mes.service.StockInService;
import com.mouse.mes.service.WarehouseService;
import com.mouse.mes.util.JsonResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StockOutControllerTest {

    /**
     * 出库控制器，用于测试出库接口返回结果。
     */
    @Autowired
    private StockOutController stockOutController;

    /**
     * 入库业务服务，用于准备可出库库存。
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
     * 测试新增出库单接口。
     */
    @Test
    public void testAddStockOut() {
        TestStockData data = prepareStock("CTRL-SOUT-ADD", new BigDecimal("10"));
        StockOut stockOut = buildStockOut("OTHER", data.warehouse.getId(), data.material.getId(), data.location.getId(), new BigDecimal("3"));
        JsonResult<StockOut> result = stockOutController.create(stockOut, warehouseIdentity());
        System.out.println("新增出库单接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 测试查询所有出库单接口。
     */
    @Test
    public void testGetAllStockOuts() {
        JsonResult<List<StockOut>> result = stockOutController.findAll();
        System.out.println("出库单列表接口数量：" + result.getData().size());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 测试库存不足时出库接口返回错误。
     */
    @Test
    public void testStockOutInsufficientShouldFail() {
        TestStockData data = prepareStock("CTRL-SOUT-NOT-ENOUGH", new BigDecimal("1"));
        StockOut stockOut = buildStockOut("OTHER", data.warehouse.getId(), data.material.getId(), data.location.getId(), new BigDecimal("5"));
        JsonResult<StockOut> result = stockOutController.create(stockOut, warehouseIdentity());
        System.out.println("库存不足出库接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
    }

    /**
     * 测试出库单不存在时删除返回错误。
     */
    @Test
    public void testDeleteStockOutNotExists() {
        JsonResult<Void> result = stockOutController.delete(-999999L);
        System.out.println("删除不存在出库单结果：" + result.getMsg());
        assertEquals(500, result.getCode());
    }

    /**
     * 测试删除出库单接口。
     */
    @Test
    public void testDeleteStockOut() {
        TestStockData data = prepareStock("CTRL-SOUT-DELETE", new BigDecimal("5"));
        StockOut stockOut = stockOutController.create(buildStockOut("OTHER", data.warehouse.getId(), data.material.getId(), data.location.getId(), new BigDecimal("1")), warehouseIdentity()).getData();
        JsonResult<Void> result = stockOutController.delete(stockOut.getId());
        System.out.println("删除出库单接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
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
        warehouse.setLocation("测试园区F栋");
        warehouse.setManagerId(1L);
        warehouse.setManagerName("仓库管理员");
        warehouse.setStatus(1);
        warehouse.setRemark("出库控制器测试仓库");
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
        material.setRemark("出库控制器测试物料");
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
        stockIn.setStockInNo("SIN-FOR-CTRL-SOUT-" + System.nanoTime());
        stockIn.setStockInType("OTHER");
        stockIn.setWarehouseId(warehouseId);
        stockIn.setStatus("COMPLETED");
        stockIn.setCreatedBy(1L);
        stockIn.setRemark("出库控制器前置入库");
        StockInItem item = new StockInItem();
        item.setMaterialId(materialId);
        item.setLocationId(locationId);
        item.setQuantity(quantity);
        item.setRemark("出库控制器前置入库明细");
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
        stockOut.setRemark("出库控制器测试单据");
        StockOutItem item = new StockOutItem();
        item.setMaterialId(materialId);
        item.setLocationId(locationId);
        item.setQuantity(quantity);
        item.setRemark("出库控制器测试明细");
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
    private jakarta.servlet.http.HttpServletRequest warehouseIdentity() {
        var request=new org.springframework.mock.web.MockHttpServletRequest();
        var user=new com.mouse.mes.dto.LoginResponse();user.setId(1L);user.setRoleId(5L);
        request.setAttribute("userInfo",user);return request;
    }

}
