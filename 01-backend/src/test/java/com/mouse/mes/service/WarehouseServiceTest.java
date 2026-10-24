package com.mouse.mes.service;

import com.mouse.mes.entity.Location;
import com.mouse.mes.entity.Warehouse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class WarehouseServiceTest {

    /**
     * 仓库业务服务，用于测试仓库新增、修改、删除和查询。
     */
    @Autowired
    private WarehouseService warehouseService;

    /**
     * 库位业务服务，用于测试仓库下的库位管理。
     */
    @Autowired
    private LocationService locationService;

    /**
     * 测试新增仓库，新增后按ID查询数据库中是否存在。
     */
    @Test
    public void testAddWarehouse() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("ADD"));
        Warehouse dbWarehouse = warehouseService.findById(warehouse.getId());
        System.out.println("新增仓库结果：" + dbWarehouse);
        assertNotNull(dbWarehouse);
        assertEquals(warehouse.getWarehouseCode(), dbWarehouse.getWarehouseCode());
    }

    /**
     * 测试修改仓库，修改后查询数据库字段是否变化。
     */
    @Test
    public void testUpdateWarehouse() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("UPDATE"));
        warehouse.setWarehouseName("修改后的仓库名称");
        warehouse.setRemark("仓库修改测试");
        Warehouse updatedWarehouse = warehouseService.update(warehouse);
        System.out.println("修改仓库结果：" + updatedWarehouse);
        assertNotNull(updatedWarehouse);
        assertEquals("修改后的仓库名称", updatedWarehouse.getWarehouseName());
        assertEquals("仓库修改测试", updatedWarehouse.getRemark());
    }

    /**
     * 测试查询所有仓库。
     */
    @Test
    public void testGetAllWarehouses() {
        List<Warehouse> warehouseList = warehouseService.findAll();
        System.out.println("仓库数量：" + warehouseList.size());
        assertNotNull(warehouseList);
    }

    /**
     * 测试按仓库编码查询仓库。
     */
    @Test
    public void testGetWarehouseByCode() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("CODE"));
        Warehouse dbWarehouse = warehouseService.findByWarehouseCode(warehouse.getWarehouseCode());
        System.out.println("按编码查询仓库结果：" + dbWarehouse);
        assertNotNull(dbWarehouse);
        assertEquals(warehouse.getId(), dbWarehouse.getId());
    }

    /**
     * 测试删除仓库，删除后按ID查询不到。
     */
    @Test
    public void testDeleteWarehouse() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("DELETE"));
        warehouseService.delete(warehouse.getId());
        Warehouse dbWarehouse = warehouseService.findById(warehouse.getId());
        System.out.println("删除仓库后查询结果：" + dbWarehouse);
        assertNull(dbWarehouse);
    }

    /**
     * 测试仓库下新增库位，并按仓库ID查询该库位。
     */
    @Test
    public void testAddLocationUnderWarehouse() {
        Warehouse warehouse = warehouseService.create(buildWarehouse("LOCATION"));
        Location location = locationService.create(buildLocation(warehouse.getId(), "LOCATION"));
        List<Location> locationList = locationService.findByWarehouseId(warehouse.getId());
        boolean exists = locationList.stream().anyMatch(item -> item.getId().equals(location.getId()));
        System.out.println("仓库下库位数量：" + locationList.size());
        assertTrue(exists);
    }

    /**
     * 构造测试仓库。
     */
    private Warehouse buildWarehouse(String scene) {
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseCode("WH-" + scene + "-" + System.nanoTime());
        warehouse.setWarehouseName("测试仓库-" + scene);
        warehouse.setLocation("测试园区A栋");
        warehouse.setManagerId(1L);
        warehouse.setManagerName("仓库管理员");
        warehouse.setStatus(1);
        warehouse.setRemark("仓库测试数据");
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
}
