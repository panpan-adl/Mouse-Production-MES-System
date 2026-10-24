package com.mouse.mes.impl;

import com.mouse.mes.constant.EquipmentStatus;
import com.mouse.mes.entity.Equipment;
import com.mouse.mes.mapper.EquipmentMapper;
import com.mouse.mes.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class EquipmentServiceImpl implements EquipmentService {

    /**
     * 设备数据库访问对象。
     */
    @Autowired
    private EquipmentMapper equipmentMapper;

    /**
     * 查询全部未删除设备。
     */
    @Override
    public List<Equipment> findAll() {
        return equipmentMapper.findAll();
    }

    /**
     * 按设备ID查询设备。
     */
    @Override
    public Equipment findById(Long id) {
        return equipmentMapper.findById(id);
    }

    /**
     * 按设备编号查询设备。
     */
    @Override
    public Equipment findByEquipmentCode(String equipmentCode) {
        return equipmentMapper.findByEquipmentCode(equipmentCode);
    }

    /**
     * 新增设备。
     */
    @Override
    public Equipment create(Equipment equipment) {
        validateEquipment(equipment);
        if (equipmentMapper.findByEquipmentCode(equipment.getEquipmentCode()) != null) {
            throw new IllegalArgumentException("设备编号已存在");
        }
        equipment.setCreateTime(new Date());
        equipment.setUpdateTime(new Date());
        if (equipment.getStatus() == null || equipment.getStatus().isBlank()) {
            equipment.setStatus(EquipmentStatus.NORMAL);
        }
        validateStatus(equipment.getStatus());
        equipment.setIsDeleted(0);
        equipmentMapper.insert(equipment);
        return equipmentMapper.findById(equipment.getId());
    }

    /**
     * 修改设备。
     */
    @Override
    public Equipment update(Equipment equipment) {
        Equipment existing = equipmentMapper.findById(equipment.getId());
        if (existing == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        mergeMissingFields(equipment, existing);
        validateEquipment(equipment);
        validateStatus(equipment.getStatus());
        equipment.setUpdateTime(new Date());
        equipmentMapper.update(equipment);
        return equipmentMapper.findById(equipment.getId());
    }

    /**
     * 逻辑删除设备。
     */
    @Override
    public void delete(Long id) {
        Equipment existing = equipmentMapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        equipmentMapper.delete(id);
    }

    /**
     * 校验设备基础字段。
     */
    private void validateEquipment(Equipment equipment) {
        if (equipment == null) {
            throw new IllegalArgumentException("设备不能为空");
        }
        if (equipment.getEquipmentCode() == null || equipment.getEquipmentCode().isBlank()) {
            throw new IllegalArgumentException("设备编号不能为空");
        }
        if (equipment.getEquipmentName() == null || equipment.getEquipmentName().isBlank()) {
            throw new IllegalArgumentException("设备名称不能为空");
        }
    }

    /**
     * 校验设备状态。
     */
    private void validateStatus(String status) {
        if (!EquipmentStatus.isValid(status)) {
            throw new IllegalArgumentException("设备状态不合法");
        }
    }

    /**
     * 修改设备时补齐未传字段，避免旧数据被空值覆盖。
     */
    private void mergeMissingFields(Equipment target, Equipment existing) {
        if (target.getEquipmentCode() == null || target.getEquipmentCode().isBlank()) {
            target.setEquipmentCode(existing.getEquipmentCode());
        }
        if (target.getEquipmentName() == null || target.getEquipmentName().isBlank()) {
            target.setEquipmentName(existing.getEquipmentName());
        }
        if (target.getEquipmentType() == null) {
            target.setEquipmentType(existing.getEquipmentType());
        }
        if (target.getModel() == null) {
            target.setModel(existing.getModel());
        }
        if (target.getManufacturer() == null) {
            target.setManufacturer(existing.getManufacturer());
        }
        if (target.getPurchaseDate() == null) {
            target.setPurchaseDate(existing.getPurchaseDate());
        }
        if (target.getWarrantyPeriod() == null) {
            target.setWarrantyPeriod(existing.getWarrantyPeriod());
        }
        if (target.getLocation() == null) {
            target.setLocation(existing.getLocation());
        }
        if (target.getStatus() == null || target.getStatus().isBlank()) {
            target.setStatus(existing.getStatus());
        }
        if (target.getResponsibleId() == null) {
            target.setResponsibleId(existing.getResponsibleId());
        }
        if (target.getResponsibleName() == null) {
            target.setResponsibleName(existing.getResponsibleName());
        }
        if (target.getRemark() == null) {
            target.setRemark(existing.getRemark());
        }
        target.setCreateTime(existing.getCreateTime());
        target.setIsDeleted(existing.getIsDeleted());
    }
}
