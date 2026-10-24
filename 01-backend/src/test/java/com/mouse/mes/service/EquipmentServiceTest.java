package com.mouse.mes.service;

import com.mouse.mes.constant.EquipmentStatus;
import com.mouse.mes.entity.Equipment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class EquipmentServiceTest {

    /**
     * 设备业务服务，用于测试设备台账新增、修改、删除和查询。
     */
    @Autowired
    private EquipmentService equipmentService;

    /**
     * 测试新增设备，新增后按ID能查到。
     */
    @Test
    public void testAddEquipment() {
        Equipment equipment = equipmentService.create(buildEquipment("ADD"));
        Equipment dbEquipment = equipmentService.findById(equipment.getId());
        System.out.println("新增设备结果：" + dbEquipment);
        assertNotNull(dbEquipment);
        assertEquals(equipment.getEquipmentCode(), dbEquipment.getEquipmentCode());
        assertEquals(EquipmentStatus.NORMAL, dbEquipment.getStatus());
    }

    /**
     * 测试修改设备状态，修改后数据库同步。
     */
    @Test
    public void testUpdateEquipmentStatus() {
        Equipment equipment = equipmentService.create(buildEquipment("UPDATE-STATUS"));
        equipment.setStatus(EquipmentStatus.REPAIRING);
        Equipment updatedEquipment = equipmentService.update(equipment);
        Equipment dbEquipment = equipmentService.findById(updatedEquipment.getId());
        System.out.println("修改设备状态结果：" + dbEquipment);
        assertEquals(EquipmentStatus.REPAIRING, dbEquipment.getStatus());
    }

    /**
     * 测试按设备编号查询设备。
     */
    @Test
    public void testFindByEquipmentCode() {
        Equipment equipment = equipmentService.create(buildEquipment("CODE"));
        Equipment dbEquipment = equipmentService.findByEquipmentCode(equipment.getEquipmentCode());
        System.out.println("按设备编号查询结果：" + dbEquipment);
        assertNotNull(dbEquipment);
        assertEquals(equipment.getId(), dbEquipment.getId());
    }

    /**
     * 测试查询所有设备。
     */
    @Test
    public void testFindAllEquipments() {
        List<Equipment> equipmentList = equipmentService.findAll();
        System.out.println("设备数量：" + equipmentList.size());
        assertNotNull(equipmentList);
    }

    /**
     * 测试删除设备后按ID查询不到。
     */
    @Test
    public void testDeleteEquipment() {
        Equipment equipment = equipmentService.create(buildEquipment("DELETE"));
        equipmentService.delete(equipment.getId());
        Equipment dbEquipment = equipmentService.findById(equipment.getId());
        System.out.println("删除设备后查询结果：" + dbEquipment);
        assertNull(dbEquipment);
    }

    /**
     * 测试删除不存在设备时返回不存在。
     */
    @Test
    public void testDeleteNotExistsShouldFail() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> equipmentService.delete(-999999L));
        System.out.println("删除不存在设备结果：" + exception.getMessage());
        assertEquals("设备不存在", exception.getMessage());
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
        equipment.setLocation("测试车间A区");
        equipment.setStatus(EquipmentStatus.NORMAL);
        equipment.setResponsibleId(1L);
        equipment.setResponsibleName("设备管理员");
        equipment.setRemark("设备测试数据");
        return equipment;
    }
}
