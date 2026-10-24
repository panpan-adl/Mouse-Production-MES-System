package com.mouse.mes.controller;

import com.mouse.mes.constant.EquipmentStatus;
import com.mouse.mes.entity.Equipment;
import com.mouse.mes.util.JsonResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EquipmentControllerTest {

    /**
     * 设备控制器，用于测试设备接口返回结果。
     */
    @Autowired
    private EquipmentController equipmentController;

    /**
     * 测试新增设备接口。
     */
    @Test
    public void testAddEquipment() {
        JsonResult<Equipment> result = equipmentController.createEquipment(buildEquipment("CTRL-ADD"));
        System.out.println("新增设备接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 测试修改设备状态接口。
     */
    @Test
    public void testUpdateEquipmentStatus() {
        Equipment equipment = equipmentController.createEquipment(buildEquipment("CTRL-UPDATE")).getData();
        equipment.setStatus(EquipmentStatus.REPAIRING);
        JsonResult<Equipment> result = equipmentController.updateEquipment(equipment.getId(), equipment);
        System.out.println("修改设备状态接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals(EquipmentStatus.REPAIRING, result.getData().getStatus());
    }

    /**
     * 测试查询全部设备接口。
     */
    @Test
    public void testFindAllEquipments() {
        JsonResult<List<Equipment>> result = equipmentController.getAllEquipments();
        System.out.println("设备列表接口数量：" + result.getData().size());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 测试删除不存在设备接口返回错误。
     */
    @Test
    public void testDeleteNotExistsShouldReturnError() {
        JsonResult<Void> result = equipmentController.deleteEquipment(-999999L);
        System.out.println("删除不存在设备接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
        assertEquals("设备不存在", result.getMsg());
    }

    /**
     * 构造测试设备。
     */
    private Equipment buildEquipment(String scene) {
        Equipment equipment = new Equipment();
        equipment.setEquipmentCode("EQ-" + scene + "-" + System.nanoTime());
        equipment.setEquipmentName("测试设备-" + scene);
        equipment.setEquipmentType("鼠标生产设备");
        equipment.setModel("MX-" + scene);
        equipment.setManufacturer("测试设备厂商");
        equipment.setPurchaseDate(new Date());
        equipment.setWarrantyPeriod(12);
        equipment.setLocation("测试车间B区");
        equipment.setStatus(EquipmentStatus.NORMAL);
        equipment.setResponsibleId(1L);
        equipment.setResponsibleName("设备管理员");
        equipment.setRemark("设备控制器测试数据");
        return equipment;
    }
}
