package com.mouse.mes.controller;

import com.mouse.mes.entity.Maintenance;
import com.mouse.mes.service.MaintenanceService;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/maintenances")
public class MaintenanceController {

    /**
     * 维修业务服务，用于维修单创建、指派、开始、完成和取消。
     */
    @Autowired
    private MaintenanceService maintenanceService;

    /**
     * 查询全部维修单。
     */
    @GetMapping
    public JsonResult<List<Maintenance>> getAllMaintenances() {
        return JsonResult.success(maintenanceService.findAll());
    }

    /**
     * 查询全部维修单，兼容/list路径。
     */
    @GetMapping("/list")
    public JsonResult<List<Maintenance>> listMaintenances() {
        return JsonResult.success(maintenanceService.findAll());
    }

    /**
     * 按维修单ID查询维修单。
     */
    @GetMapping("/{id}")
    public JsonResult<Maintenance> getMaintenanceById(@PathVariable Long id) {
        Maintenance maintenance = maintenanceService.findById(id);
        if (maintenance == null) {
            return JsonResult.error("维修单不存在");
        }
        return JsonResult.success(maintenance);
    }

    /**
     * 按维修单号查询维修单。
     */
    @GetMapping("/no/{maintenanceNo}")
    public JsonResult<Maintenance> getMaintenanceByNo(@PathVariable String maintenanceNo) {
        Maintenance maintenance = maintenanceService.findByMaintenanceNo(maintenanceNo);
        if (maintenance == null) {
            return JsonResult.error("维修单不存在");
        }
        return JsonResult.success(maintenance);
    }

    /**
     * 按设备ID查询维修单。
     */
    @GetMapping("/equipment/{equipmentId}")
    public JsonResult<List<Maintenance>> getMaintenancesByEquipmentId(@PathVariable Long equipmentId) {
        return JsonResult.success(maintenanceService.findByEquipmentId(equipmentId));
    }

    /**
     * 创建维修单。
     */
    @PostMapping
    public JsonResult<Maintenance> createMaintenance(@RequestBody Maintenance maintenance) {
        try {
            return JsonResult.success(maintenanceService.create(maintenance));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 修改维修单基础信息。
     */
    @PutMapping("/{id}")
    public JsonResult<Maintenance> updateMaintenance(@PathVariable Long id, @RequestBody Maintenance maintenance) {
        try {
            maintenance.setId(id);
            return JsonResult.success(maintenanceService.update(maintenance));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 指派维修人员。
     */
    @PutMapping("/{id}/assign")
    public JsonResult<Maintenance> assign(@PathVariable Long id, @RequestBody AssignRequest request) {
        try {
            return JsonResult.success(maintenanceService.assign(id, request.getUserId(), request.getUserName()));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 开始维修。
     */
    @PutMapping("/{id}/start")
    public JsonResult<Maintenance> start(@PathVariable Long id) {
        try {
            return JsonResult.success(maintenanceService.start(id));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 完成维修。
     */
    @PutMapping("/{id}/finish")
    public JsonResult<Maintenance> finish(@PathVariable Long id, @RequestBody FinishRequest request) {
        try {
            return JsonResult.success(maintenanceService.finish(
                    id, request.getMaintenanceContent(), request.getResult(), request.getCost(), request.getScrapEquipment()));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 取消维修。
     */
    @PutMapping("/{id}/cancel")
    public JsonResult<Maintenance> cancel(@PathVariable Long id) {
        try {
            return JsonResult.success(maintenanceService.cancel(id));
        } catch (IllegalArgumentException exception) {
            return JsonResult.error(exception.getMessage());
        }
    }

    /**
     * 删除维修单。
     */
    @DeleteMapping("/{id}")
    public JsonResult<Void> deleteMaintenance(@PathVariable Long id) {
        maintenanceService.delete(id);
        return JsonResult.success();
    }

    /**
     * 指派维修人员请求体。
     */
    public static class AssignRequest {

        /**
         * 维修人员ID。
         */
        private Long userId;

        /**
         * 维修人员姓名。
         */
        private String userName;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    /**
     * 完成维修请求体。
     */
    public static class FinishRequest {

        /**
         * 维修内容。
         */
        private String maintenanceContent;

        /**
         * 维修结果。
         */
        private String result;

        /**
         * 维修费用。
         */
        private BigDecimal cost;

        /**
         * 是否将设备标记为报废。
         */
        private Boolean scrapEquipment;

        public String getMaintenanceContent() {
            return maintenanceContent;
        }

        public void setMaintenanceContent(String maintenanceContent) {
            this.maintenanceContent = maintenanceContent;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public BigDecimal getCost() {
            return cost;
        }

        public void setCost(BigDecimal cost) {
            this.cost = cost;
        }

        public Boolean getScrapEquipment() {
            return scrapEquipment;
        }

        public void setScrapEquipment(Boolean scrapEquipment) {
            this.scrapEquipment = scrapEquipment;
        }
    }
}
