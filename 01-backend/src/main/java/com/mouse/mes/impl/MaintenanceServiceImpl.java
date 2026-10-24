package com.mouse.mes.impl;

import com.mouse.mes.constant.EquipmentStatus;
import com.mouse.mes.constant.MaintenanceStatus;
import com.mouse.mes.entity.Equipment;
import com.mouse.mes.entity.Maintenance;
import com.mouse.mes.entity.User;
import com.mouse.mes.mapper.EquipmentMapper;
import com.mouse.mes.mapper.MaintenanceMapper;
import com.mouse.mes.mapper.UserMapper;
import com.mouse.mes.service.MaintenanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class MaintenanceServiceImpl implements MaintenanceService {

    /** 设备维护员在系统角色表中的固定角色ID。 */
    private static final long EQUIPMENT_MAINTAINER_ROLE_ID = 8L;

    /**
     * 维修单数据库访问对象。
     */
    @Autowired
    private MaintenanceMapper maintenanceMapper;

    /**
     * 设备数据库访问对象，用于维修流转时同步设备状态。
     */
    @Autowired
    private EquipmentMapper equipmentMapper;

    /** 员工数据库访问对象，用于校验维修人员身份和在职状态。 */
    @Autowired
    private UserMapper userMapper;

    /**
     * 查询全部维修单。
     */
    @Override
    public List<Maintenance> findAll() {
        return maintenanceMapper.findAll();
    }

    /**
     * 按维修单ID查询维修单。
     */
    @Override
    public Maintenance findById(Long id) {
        return maintenanceMapper.findById(id);
    }

    /**
     * 按维修单号查询维修单。
     */
    @Override
    public Maintenance findByMaintenanceNo(String maintenanceNo) {
        return maintenanceMapper.findByMaintenanceNo(maintenanceNo);
    }

    /**
     * 按设备ID查询维修单。
     */
    @Override
    public List<Maintenance> findByEquipmentId(Long equipmentId) {
        return maintenanceMapper.findByEquipmentId(equipmentId);
    }

    /**
     * 创建设备维修单；创建后设备状态变为维修中。
     */
    @Override
    @Transactional
    public Maintenance create(Maintenance maintenance) {
        validateCreate(maintenance);
        Equipment equipment = equipmentMapper.findById(maintenance.getEquipmentId());
        validateEquipmentCanMaintain(equipment);
        if (!Integer.valueOf(1).equals(maintenance.getMaintenanceType()) && maintenanceMapper.findUnfinishedByEquipmentId(maintenance.getEquipmentId()) != null) {
            throw new IllegalArgumentException("该设备已有未完成维修单，不能重复创建");
        }
        if (maintenanceMapper.findByMaintenanceNo(maintenance.getMaintenanceNo()) != null) {
            throw new IllegalArgumentException("维修单号已存在");
        }
        maintenance.setEquipmentName(equipment.getEquipmentName());
        maintenance.setStatus(MaintenanceStatus.PENDING);
        maintenance.setCreateTime(new Date());
        maintenance.setUpdateTime(new Date());
        maintenanceMapper.insert(maintenance);
        if (!Integer.valueOf(1).equals(maintenance.getMaintenanceType())) updateEquipmentStatus(equipment, EquipmentStatus.REPAIR_REQUESTED);
        return maintenanceMapper.findById(maintenance.getId());
    }

    /**
     * 修改维修单基础信息。
     */
    @Override
    public Maintenance update(Maintenance maintenance) {
        Maintenance existing = maintenanceMapper.findById(maintenance.getId());
        if (existing == null) {
            throw new IllegalArgumentException("维修单不存在");
        }
        mergeMissingFields(maintenance, existing);
        if(!java.util.Objects.equals(maintenance.getStatus(),existing.getStatus())||!java.util.Objects.equals(maintenance.getEquipmentId(),existing.getEquipmentId())||!java.util.Objects.equals(maintenance.getMaintenanceUserId(),existing.getMaintenanceUserId()))
            throw new IllegalArgumentException("不能直接修改维修状态、设备或负责人，请使用指派、开始和完成操作");
        if(MaintenanceStatus.FINISHED.equals(existing.getStatus())||MaintenanceStatus.CANCELLED.equals(existing.getStatus()))throw new IllegalArgumentException("已关闭维修单保留历史，不可修改");
        if (!MaintenanceStatus.isValid(maintenance.getStatus())) {
            throw new IllegalArgumentException("维修单状态不合法");
        }
        maintenance.setUpdateTime(new Date());
        maintenanceMapper.update(maintenance);
        return maintenanceMapper.findById(maintenance.getId());
    }

    /**
     * 指派维修人员。
     */
    @Override
    public Maintenance assign(Long id, Long userId, String userName) {
        Maintenance maintenance = requireEditableMaintenance(id);
        User maintainer = requireActiveMaintainer(userId);
        maintenance.setMaintenanceUserId(maintainer.getId());
        maintenance.setMaintenanceUserName(maintainer.getRealName());
        maintenance.setUpdateTime(new Date());
        maintenanceMapper.update(maintenance);
        return maintenanceMapper.findById(id);
    }

    /**
     * 开始维修。
     */
    @Override
    @Transactional
    public Maintenance start(Long id) {
        Maintenance maintenance = requireEditableMaintenance(id);
        if (maintenance.getMaintenanceUserId() == null) {
            throw new IllegalArgumentException("请先指派维修人员，再开始维修");
        }
        maintenance.setStatus(MaintenanceStatus.PROCESSING);
        maintenance.setStartTime(new Date());
        maintenance.setUpdateTime(new Date());
        maintenanceMapper.update(maintenance);
        Equipment equipment = equipmentMapper.findById(maintenance.getEquipmentId());
        updateEquipmentStatus(equipment, EquipmentStatus.REPAIRING);
        return maintenanceMapper.findById(id);
    }

    /**
     * 完成维修；完成后设备恢复正常或标记报废。
     */
    @Override
    @Transactional
    public Maintenance finish(Long id, String maintenanceContent, String result, BigDecimal cost, Boolean scrapEquipment) {
        Maintenance maintenance = requireEditableMaintenance(id);
        if (maintenanceContent == null || maintenanceContent.isBlank()) {
            throw new IllegalArgumentException("维修内容不能为空");
        }
        if(cost!=null&&cost.signum()<0)throw new IllegalArgumentException("维修费用不能为负数");
        maintenance.setMaintenanceContent(maintenanceContent);
        maintenance.setResult(result == null || result.isBlank() ? "REPAIRED" : result);
        maintenance.setCost(cost == null ? BigDecimal.ZERO : cost);
        maintenance.setStatus(MaintenanceStatus.FINISHED);
        if (maintenance.getStartTime() == null) {
            maintenance.setStartTime(new Date());
        }
        maintenance.setEndTime(new Date());
        maintenance.setUpdateTime(new Date());
        maintenanceMapper.update(maintenance);

        Equipment equipment = equipmentMapper.findById(maintenance.getEquipmentId());
        boolean shouldScrap = Boolean.TRUE.equals(scrapEquipment) || "SCRAPPED".equals(maintenance.getResult());
        updateEquipmentStatus(equipment, shouldScrap ? EquipmentStatus.SCRAPPED : EquipmentStatus.NORMAL);
        return maintenanceMapper.findById(id);
    }

    /**
     * 取消维修单；取消后设备回到故障状态，表示仍需处理。
     */
    @Override
    @Transactional
    public Maintenance cancel(Long id) {
        Maintenance maintenance = requireEditableMaintenance(id);
        maintenance.setStatus(MaintenanceStatus.CANCELLED);
        maintenance.setUpdateTime(new Date());
        maintenanceMapper.update(maintenance);
        Equipment equipment = equipmentMapper.findById(maintenance.getEquipmentId());
        updateEquipmentStatus(equipment, EquipmentStatus.REPAIR_REQUESTED);
        return maintenanceMapper.findById(id);
    }

    /**
     * 校验创建设备维修单字段。
     */
    private void validateCreate(Maintenance maintenance) {
        if (maintenance == null) {
            throw new IllegalArgumentException("维修单不能为空");
        }
        if (maintenance.getMaintenanceNo() == null || maintenance.getMaintenanceNo().isBlank()) {
            throw new IllegalArgumentException("维修单号不能为空");
        }
        if (maintenance.getEquipmentId() == null) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
        if (maintenance.getMaintenanceType() == null) {
            maintenance.setMaintenanceType(2);
        }
    }

    /**
     * 校验设备是否可以创建维修单。
     */
    private void validateEquipmentCanMaintain(Equipment equipment) {
        if (equipment == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        if (EquipmentStatus.SCRAPPED.equals(equipment.getStatus())) {
            throw new IllegalArgumentException("报废设备不能创建维修单");
        }
    }

    /**
     * 校验被指派员工是当前在职的设备维护员，不能仅相信前端传入的姓名。
     */
    @Autowired private com.mouse.mes.service.RoleAccessService roleAccess;
    private User requireActiveMaintainer(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("请选择维修人员");
        }
        User user = userMapper.findById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            throw new IllegalArgumentException("维修人员不存在或已停用");
        }
        if (user.getRoleId() == null || roleAccess.effective(user.getRoleId()) != EQUIPMENT_MAINTAINER_ROLE_ID) {
            throw new IllegalArgumentException("只能指派设备维护员处理维修单");
        }
        return user;
    }

    /**
     * 查询可编辑维修单；已完成和已取消的维修单不能再流转。
     */
    private Maintenance requireEditableMaintenance(Long id) {
        Maintenance maintenance = maintenanceMapper.findById(id);
        if (maintenance == null) {
            throw new IllegalArgumentException("维修单不存在");
        }
        if (MaintenanceStatus.FINISHED.equals(maintenance.getStatus())) {
            throw new IllegalArgumentException("维修单已完成，不能重复操作");
        }
        if (MaintenanceStatus.CANCELLED.equals(maintenance.getStatus())) {
            throw new IllegalArgumentException("维修单已取消，不能继续操作");
        }
        return maintenance;
    }

    /**
     * 同步设备状态。
     */
    private void updateEquipmentStatus(Equipment equipment, String status) {
        if (equipment == null) {
            return;
        }
        equipment.setStatus(status);
        equipment.setUpdateTime(new Date());
        equipmentMapper.update(equipment);
    }

    /**
     * 修改维修单时补齐未传字段。
     */
    private void mergeMissingFields(Maintenance target, Maintenance existing) {
        if (target.getMaintenanceNo() == null || target.getMaintenanceNo().isBlank()) {
            target.setMaintenanceNo(existing.getMaintenanceNo());
        }
        if (target.getEquipmentId() == null) {
            target.setEquipmentId(existing.getEquipmentId());
        }
        if (target.getEquipmentName() == null) {
            target.setEquipmentName(existing.getEquipmentName());
        }
        if (target.getMaintenanceType() == null) {
            target.setMaintenanceType(existing.getMaintenanceType());
        }
        if (target.getFaultDesc() == null) {
            target.setFaultDesc(existing.getFaultDesc());
        }
        if (target.getMaintenanceContent() == null) {
            target.setMaintenanceContent(existing.getMaintenanceContent());
        }
        if (target.getMaintenanceUserId() == null) {
            target.setMaintenanceUserId(existing.getMaintenanceUserId());
        }
        if (target.getMaintenanceUserName() == null) {
            target.setMaintenanceUserName(existing.getMaintenanceUserName());
        }
        if (target.getStartTime() == null) {
            target.setStartTime(existing.getStartTime());
        }
        if (target.getEndTime() == null) {
            target.setEndTime(existing.getEndTime());
        }
        if (target.getCost() == null) {
            target.setCost(existing.getCost());
        }
        if (target.getResult() == null) {
            target.setResult(existing.getResult());
        }
        if (target.getStatus() == null || target.getStatus().isBlank()) {
            target.setStatus(existing.getStatus());
        }
        if (target.getRemark() == null) {
            target.setRemark(existing.getRemark());
        }
        target.setCreateTime(existing.getCreateTime());
    }

    @Override
    public void delete(Long id) {
        maintenanceMapper.delete(id);
    }
}
