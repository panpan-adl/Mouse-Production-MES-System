package com.mouse.mes.controller;

import com.mouse.mes.constant.EquipmentStatus;
import com.mouse.mes.constant.MaintenanceStatus;
import com.mouse.mes.entity.Equipment;
import com.mouse.mes.entity.Maintenance;
import com.mouse.mes.service.EquipmentService;
import com.mouse.mes.util.JsonResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MaintenanceControllerTest {

    /**
     * 维修控制器，用于测试维修单接口返回结果。
     */
    @Autowired
    private MaintenanceController maintenanceController;

    /**
     * 设备业务服务，用于准备测试设备并验证设备状态。
     */
    @Autowired
    private EquipmentService equipmentService;

    /**
     * 测试创建维修单接口，创建后设备状态变为维修中。
     */
    @Test
    public void testCreateMaintenanceAndEquipmentMaintaining() {
        Equipment equipment = equipmentService.create(buildEquipment("CTRL-MT-CREATE", EquipmentStatus.NORMAL));
        JsonResult<Maintenance> result = maintenanceController.createMaintenance(buildMaintenance("CTRL-MT-CREATE", equipment.getId()));
        Equipment dbEquipment = equipmentService.findById(equipment.getId());
        System.out.println("创建维修单接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals(MaintenanceStatus.PENDING, result.getData().getStatus());
        assertEquals(EquipmentStatus.REPAIR_REQUESTED, dbEquipment.getStatus());
    }

    /**
     * 测试完成维修接口，维修单状态变为已完成。
     */
    @Test
    public void testFinishMaintenance() {
        Equipment equipment = equipmentService.create(buildEquipment("CTRL-MT-FINISH", EquipmentStatus.NORMAL));
        Maintenance maintenance = maintenanceController.createMaintenance(buildMaintenance("CTRL-MT-FINISH", equipment.getId())).getData();
        maintenanceController.start(maintenance.getId());
        MaintenanceController.FinishRequest request = new MaintenanceController.FinishRequest();
        request.setMaintenanceContent("清洁设备并更换滚轮夹具");
        request.setResult("REPAIRED");
        request.setCost(new BigDecimal("88.00"));
        request.setScrapEquipment(false);
        JsonResult<Maintenance> result = maintenanceController.finish(maintenance.getId(), request);
        Equipment dbEquipment = equipmentService.findById(equipment.getId());
        System.out.println("完成维修接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals(MaintenanceStatus.FINISHED, result.getData().getStatus());
        assertEquals(EquipmentStatus.NORMAL, dbEquipment.getStatus());
    }

    /**
     * 测试同一设备不能重复创建未完成维修单接口。
     */
    @Test
    public void testCannotCreateDuplicateUnfinishedMaintenance() {
        Equipment equipment = equipmentService.create(buildEquipment("CTRL-MT-DUP", EquipmentStatus.NORMAL));
        maintenanceController.createMaintenance(buildMaintenance("CTRL-MT-DUP-1", equipment.getId()));
        JsonResult<Maintenance> result = maintenanceController.createMaintenance(buildMaintenance("CTRL-MT-DUP-2", equipment.getId()));
        System.out.println("重复创建维修单接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
        assertEquals("该设备已有未完成维修单，不能重复创建", result.getMsg());
    }

    /**
     * 测试报废设备不能创建维修单接口。
     */
    @Test
    public void testScrappedEquipmentCannotCreateMaintenance() {
        Equipment equipment = equipmentService.create(buildEquipment("CTRL-MT-SCRAPPED", EquipmentStatus.SCRAPPED));
        JsonResult<Maintenance> result = maintenanceController.createMaintenance(buildMaintenance("CTRL-MT-SCRAPPED", equipment.getId()));
        System.out.println("报废设备创建维修单接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
        assertEquals("报废设备不能创建维修单", result.getMsg());
    }

    /**
     * 测试按维修单ID查询接口。
     */
    @Test
    public void testFindMaintenanceById() {
        Equipment equipment = equipmentService.create(buildEquipment("CTRL-MT-FIND", EquipmentStatus.NORMAL));
        Maintenance maintenance = maintenanceController.createMaintenance(buildMaintenance("CTRL-MT-FIND", equipment.getId())).getData();
        JsonResult<Maintenance> result = maintenanceController.getMaintenanceById(maintenance.getId());
        System.out.println("查询维修单接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 构造测试设备。
     */
    private Equipment buildEquipment(String scene, String status) {
        Equipment equipment = new Equipment();
        equipment.setEquipmentCode("EQ-" + scene + "-" + System.nanoTime());
        equipment.setEquipmentName("测试设备-" + scene);
        equipment.setEquipmentType("鼠标生产设备");
        equipment.setModel("MX-" + scene);
        equipment.setManufacturer("测试设备厂商");
        equipment.setPurchaseDate(new Date());
        equipment.setWarrantyPeriod(12);
        equipment.setLocation("维修控制器测试车间");
        equipment.setStatus(status);
        equipment.setResponsibleId(1L);
        equipment.setResponsibleName("设备管理员");
        equipment.setRemark("维修控制器测试设备");
        return equipment;
    }

    /**
     * 构造测试维修单。
     */
    private Maintenance buildMaintenance(String scene, Long equipmentId) {
        Maintenance maintenance = new Maintenance();
        maintenance.setMaintenanceNo("MT-" + scene + "-" + System.nanoTime());
        maintenance.setEquipmentId(equipmentId);
        maintenance.setMaintenanceType(2);
        maintenance.setFaultDesc("设备运行异常");
        maintenance.setRemark("维修控制器测试数据");
        return maintenance;
    }
}
