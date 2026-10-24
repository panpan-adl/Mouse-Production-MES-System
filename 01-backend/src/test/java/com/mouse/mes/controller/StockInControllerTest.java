package com.mouse.mes.controller;

import com.mouse.mes.entity.Location;
import com.mouse.mes.entity.Material;
import com.mouse.mes.entity.StockIn;
import com.mouse.mes.entity.StockInItem;
import com.mouse.mes.entity.Warehouse;
import com.mouse.mes.mapper.MaterialMapper;
import com.mouse.mes.service.LocationService;
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
public class StockInControllerTest {

    /**
     * 入库控制器，用于测试入库接口返回结果。
     */
    @Autowired
    private StockInController stockInController;

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
     * 测试新增入库单接口。
     */
    @Test
    public void testAddStockIn() {
        StockIn stockIn = prepareStockIn("CTRL-SIN-ADD", "OTHER", new BigDecimal("12"));
        JsonResult<StockIn> result = stockInController.create(stockIn, warehouseIdentity());
        System.out.println("新增入库单接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 测试查询所有入库单接口。
     */
    @Test
    public void testGetAllStockIns() {
        JsonResult<List<StockIn>> result = stockInController.findAll();
        System.out.println("入库单列表接口数量：" + result.getData().size());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 测试入库单不存在时查询返回错误。
     */
    @Test
    public void testFindStockInNotExists() {
        JsonResult<StockIn> result = stockInController.findById(-999999L);
        System.out.println("查询不存在入库单结果：" + result.getMsg());
        assertEquals(500, result.getCode());
    }

    /**
     * 测试删除入库单接口。
     */
    @Test
    public void testDeleteStockIn() {
        StockIn stockIn = stockInController.create(prepareStockIn("CTRL-SIN-DELETE", "OTHER", new BigDecimal("4")), warehouseIdentity()).getData();
        JsonResult<Void> result = stockInController.delete(stockIn.getId());
        System.out.println("删除入库单接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
    }

    /**
     * 准备测试入库单。
     */
    private StockIn prepareStockIn(String scene, String stockInType, BigDecimal quantity) {
        Warehouse warehouse = warehouseService.create(buildWarehouse(scene));
        Location location = locationService.create(buildLocation(warehouse.getId(), scene));
        Material material = createMaterial(scene, 5);
        StockIn stockIn = new StockIn();
        stockIn.setStockInNo("SIN-" + scene + "-" + System.nanoTime());
        stockIn.setStockInType(stockInType);
        stockIn.setWarehouseId(warehouse.getId());
        stockIn.setSourceOrderNo("SOURCE-" + System.nanoTime());
        stockIn.setStatus("COMPLETED");
        stockIn.setCreatedBy(1L);
        stockIn.setRemark("入库控制器测试单据");
        StockInItem item = new StockInItem();
        item.setMaterialId(material.getId());
        item.setLocationId(location.getId());
        item.setQuantity(quantity);
        item.setRemark("入库控制器测试明细");
        stockIn.setItems(Collections.singletonList(item));
        return stockIn;
    }

    /**
     * 构造测试仓库。
     */
    private Warehouse buildWarehouse(String scene) {
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseCode("WH-" + scene + "-" + System.nanoTime());
        warehouse.setWarehouseName("测试仓库-" + scene);
        warehouse.setLocation("测试园区D栋");
        warehouse.setManagerId(1L);
        warehouse.setManagerName("仓库管理员");
        warehouse.setStatus(1);
        warehouse.setRemark("入库控制器测试仓库");
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
        material.setRemark("入库控制器测试物料");
        material.setCreateTime(new Date());
        material.setUpdateTime(new Date());
        materialMapper.insert(material);
        return material;
    }
    private jakarta.servlet.http.HttpServletRequest warehouseIdentity() {
        var request=new org.springframework.mock.web.MockHttpServletRequest();
        var user=new com.mouse.mes.dto.LoginResponse();user.setId(1L);user.setRoleId(5L);
        request.setAttribute("userInfo",user);return request;
    }

}
