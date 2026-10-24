package com.mouse.mes.service;

import com.mouse.mes.constant.EquipmentStatus;
import com.mouse.mes.constant.MaintenanceStatus;
import com.mouse.mes.entity.Equipment;
import com.mouse.mes.entity.Maintenance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class MaintenanceServiceTest {

    /**
     * 维修业务服务，用于测试维修单创建、指派、开始和完成。
     */
    @Autowired
    private MaintenanceService maintenanceService;

    /**
     * 设备业务服务，用于准备测试设备并验证设备状态同步。
     */
    @Autowired
    private EquipmentService equipmentService;

    /**
     * 测试创建设备维修单后，设备状态变为维修中。
     */
    @Test
    public void testCreateMaintenanceAndEquipmentMaintaining() {
        Equipment equipment = equipmentService.create(buildEquipment("MT-CREATE", EquipmentStatus.NORMAL));
        Maintenance maintenance = maintenanceService.create(buildMaintenance("MT-CREATE", equipment.getId()));
        Equipment dbEquipment = equipmentService.findById(equipment.getId());
        System.out.println("创建维修单结果：" + maintenance);
        System.out.println("创建维修单后设备状态：" + dbEquipment.getStatus());
        assertEquals(MaintenanceStatus.PENDING, maintenance.getStatus());
        assertEquals(EquipmentStatus.REPAIR_REQUESTED, dbEquipment.getStatus());
    }

    /**
     * 测试指派维修人员并开始维修。
     */
    @Test
    public void testAssignAndStartMaintenance() {
        Equipment equipment = equipmentService.create(buildEquipment("MT-START", EquipmentStatus.NORMAL));
        Maintenance maintenance = maintenanceService.create(buildMaintenance("MT-START", equipment.getId()));
        maintenance = maintenanceService.assign(maintenance.getId(), 1001L, "维修人员A");
        maintenance = maintenanceService.start(maintenance.getId());
        System.out.println("开始维修结果：" + maintenance);
        assertEquals(1001L, maintenance.getMaintenanceUserId());
        assertEquals(MaintenanceStatus.PROCESSING, maintenance.getStatus());
        assertNotNull(maintenance.getStartTime());
    }

    /**
     * 测试完成维修后，维修单状态变为已完成，设备恢复正常。
     */
    @Test
    public void testFinishMaintenanceAndEquipmentNormal() {
        Equipment equipment = equipmentService.create(buildEquipment("MT-FINISH", EquipmentStatus.NORMAL));
        Maintenance maintenance = maintenanceService.create(buildMaintenance("MT-FINISH", equipment.getId()));
        maintenanceService.start(maintenance.getId());
        Maintenance finishedMaintenance = maintenanceService.finish(
                maintenance.getId(), "更换传感器并测试通过", "REPAIRED", new BigDecimal("120.50"), false);
        Equipment dbEquipment = equipmentService.findById(equipment.getId());
        System.out.println("完成维修结果：" + finishedMaintenance);
        System.out.println("完成维修后设备状态：" + dbEquipment.getStatus());
        assertEquals(MaintenanceStatus.FINISHED, finishedMaintenance.getStatus());
        assertEquals(EquipmentStatus.NORMAL, dbEquipment.getStatus());
    }

    /**
     * 测试完成维修后可以将设备标记为报废。
     */
    @Test
    public void testFinishMaintenanceAndEquipmentScrapped() {
        Equipment equipment = equipmentService.create(buildEquipment("MT-SCRAP-FINISH", EquipmentStatus.NORMAL));
        Maintenance maintenance = maintenanceService.create(buildMaintenance("MT-SCRAP-FINISH", equipment.getId()));
        Maintenance finishedMaintenance = maintenanceService.finish(
                maintenance.getId(), "主板严重烧毁，无法维修", "SCRAPPED", new BigDecimal("30"), true);
        Equipment dbEquipment = equipmentService.findById(equipment.getId());
        System.out.println("报废维修结果：" + finishedMaintenance);
        assertEquals(MaintenanceStatus.FINISHED, finishedMaintenance.getStatus());
        assertEquals(EquipmentStatus.SCRAPPED, dbEquipment.getStatus());
    }

    /**
     * 测试同一设备不能重复创建未完成维修单。
     */
    @Test
    public void testCannotCreateDuplicateUnfinishedMaintenance() {
        Equipment equipment = equipmentService.create(buildEquipment("MT-DUP", EquipmentStatus.NORMAL));
        maintenanceService.create(buildMaintenance("MT-DUP-1", equipment.getId()));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceService.create(buildMaintenance("MT-DUP-2", equipment.getId())));
        System.out.println("重复创建维修单结果：" + exception.getMessage());
        assertEquals("该设备已有未完成维修单，不能重复创建", exception.getMessage());
    }

    /**
     * 测试报废设备不能创建维修单。
     */
    @Test
    public void testScrappedEquipmentCannotCreateMaintenance() {
        Equipment equipment = equipmentService.create(buildEquipment("MT-SCRAPPED", EquipmentStatus.SCRAPPED));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceService.create(buildMaintenance("MT-SCRAPPED", equipment.getId())));
        System.out.println("报废设备创建维修单结果：" + exception.getMessage());
        assertEquals("报废设备不能创建维修单", exception.getMessage());
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
        equipment.setLocation("维修测试车间");
        equipment.setStatus(status);
        equipment.setResponsibleId(1L);
        equipment.setResponsibleName("设备管理员");
        equipment.setRemark("维修测试设备");
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
        maintenance.setRemark("维修测试数据");
        return maintenance;
    }
}
