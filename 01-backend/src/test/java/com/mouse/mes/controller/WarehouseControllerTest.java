package com.mouse.mes.controller;

import com.mouse.mes.entity.Warehouse;
import com.mouse.mes.util.JsonResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WarehouseControllerTest {

    /**
     * 仓库控制器，用于测试仓库接口返回结果。
     */
    @Autowired
    private WarehouseController warehouseController;

    /**
     * 测试新增仓库接口，新增后返回成功和仓库数据。
     */
    @Test
    public void testAddWarehouse() {
        JsonResult<Warehouse> result = warehouseController.createWarehouse(buildWarehouse("CTRL-ADD"));
        System.out.println("新增仓库接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 测试查询所有仓库接口。
     */
    @Test
    public void testGetAllWarehouses() {
        JsonResult<List<Warehouse>> result = warehouseController.getAllWarehouses();
        System.out.println("仓库列表接口数量：" + result.getData().size());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 测试修改仓库接口。
     */
    @Test
    public void testUpdateWarehouse() {
        Warehouse warehouse = warehouseController.createWarehouse(buildWarehouse("CTRL-UPDATE")).getData();
        warehouse.setWarehouseName("控制器修改仓库");
        JsonResult<Warehouse> result = warehouseController.updateWarehouse(warehouse.getId(), warehouse);
        System.out.println("修改仓库接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals("控制器修改仓库", result.getData().getWarehouseName());
    }

    /**
     * 测试删除仓库接口，删除后再次查询应返回错误。
     */
    @Test
    public void testDeleteWarehouse() {
        Warehouse warehouse = warehouseController.createWarehouse(buildWarehouse("CTRL-DELETE")).getData();
        JsonResult<Void> deleteResult = warehouseController.deleteWarehouse(warehouse.getId());
        JsonResult<Warehouse> findResult = warehouseController.getWarehouseById(warehouse.getId());
        System.out.println("删除仓库接口结果：" + deleteResult.getMsg());
        assertEquals(200, deleteResult.getCode());
        assertEquals(500, findResult.getCode());
    }

    /**
     * 构造测试仓库。
     */
    private Warehouse buildWarehouse(String scene) {
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseCode("WH-" + scene + "-" + System.nanoTime());
        warehouse.setWarehouseName("测试仓库-" + scene);
        warehouse.setLocation("测试园区B栋");
        warehouse.setManagerId(1L);
        warehouse.setManagerName("仓库管理员");
        warehouse.setStatus(1);
        warehouse.setRemark("仓库控制器测试数据");
        return warehouse;
    }
}
