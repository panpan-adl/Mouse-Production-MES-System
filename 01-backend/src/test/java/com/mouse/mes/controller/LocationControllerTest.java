package com.mouse.mes.controller;

import com.mouse.mes.entity.Location;
import com.mouse.mes.entity.Warehouse;
import com.mouse.mes.service.WarehouseService;
import com.mouse.mes.util.JsonResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LocationControllerTest {

    @Autowired
    private LocationController locationController;

    @Autowired
    private WarehouseService warehouseService;

    @Test
    public void testAddLocation() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("CTRL-LOC-ADD"));
        JsonResult<Location> result = locationController.create(buildLocation(warehouse.getId(), "CTRL-ADD"));
        System.out.println("库位新增接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    public void testGetAllLocations() {
        JsonResult<List<Location>> result = locationController.findAll();
        System.out.println("库位列表接口数量：" + result.getData().size());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    public void testGetLocationById() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("CTRL-LOC-FIND"));
        Location saved = locationController.create(buildLocation(warehouse.getId(), "CTRL-FIND")).getData();
        JsonResult<Location> result = locationController.findById(saved.getId());
        System.out.println("库位查询接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals(saved.getLocationCode(), result.getData().getLocationCode());
    }

    @Test
    public void testGetLocationByIdNotFound() {
        JsonResult<Location> result = locationController.findById(-999999L);
        System.out.println("库位不存在接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
    }

    @Test
    public void testGetLocationsByWarehouseId() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("CTRL-LOC-WH"));
        locationController.create(buildLocation(warehouse.getId(), "CTRL-WH"));
        JsonResult<List<Location>> result = locationController.findByWarehouseId(warehouse.getId());
        System.out.println("按仓库查询库位接口数量：" + result.getData().size());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    public void testUpdateLocation() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("CTRL-LOC-UPDATE"));
        Location saved = locationController.create(buildLocation(warehouse.getId(), "CTRL-UPDATE")).getData();
        saved.setLocationName("控制器修改库位");
        JsonResult<Location> result = locationController.update(saved.getId(), saved);
        System.out.println("库位修改接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals("控制器修改库位", result.getData().getLocationName());
    }

    @Test
    public void testDeleteLocation() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("CTRL-LOC-DELETE"));
        Location saved = locationController.create(buildLocation(warehouse.getId(), "CTRL-DELETE")).getData();
        JsonResult<Void> result = locationController.delete(saved.getId());
        JsonResult<Location> findResult = locationController.findById(saved.getId());
        System.out.println("库位删除接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals(500, findResult.getCode());
    }

    private Warehouse buildWarehouse(String scene) {
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseCode("WH-" + scene + "-" + System.nanoTime());
        warehouse.setWarehouseName("测试仓库-" + scene);
        warehouse.setLocation("测试园区");
        warehouse.setManagerId(1L);
        warehouse.setManagerName("仓库管理员");
        warehouse.setStatus(1);
        warehouse.setRemark("库位控制器测试仓库");
        return warehouse;
    }

    private Location buildLocation(Long warehouseId, String scene) {
        Location location = new Location();
        location.setWarehouseId(warehouseId);
        location.setLocationCode("LOC-" + scene + "-" + System.nanoTime());
        location.setLocationName("测试库位-" + scene);
        location.setLocationType("NORMAL");
        location.setStatus("1");
        return location;
    }
}