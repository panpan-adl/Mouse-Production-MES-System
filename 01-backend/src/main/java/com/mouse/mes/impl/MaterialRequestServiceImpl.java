package com.mouse.mes.impl;

import com.mouse.mes.entity.Inventory;
import com.mouse.mes.entity.Material;
import com.mouse.mes.entity.MaterialRequest;
import com.mouse.mes.mapper.InventoryMapper;
import com.mouse.mes.mapper.MaterialMapper;
import com.mouse.mes.mapper.MaterialRequestMapper;
import com.mouse.mes.service.MaterialRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class MaterialRequestServiceImpl implements MaterialRequestService {

    private static final String STATUS_ENOUGH = "ENOUGH";
    private static final String STATUS_SHORTAGE = "SHORTAGE";
    private static final String STATUS_PURCHASE_REQUESTED = "PURCHASE_REQUESTED";
    private static final String STATUS_PURCHASE_ORDERED = "PURCHASE_ORDERED";

    @Autowired
    private MaterialRequestMapper materialRequestMapper;

    /** 用于读取物料编码和名称。 */
    @Autowired
    private MaterialMapper materialMapper;

    /** 用于计算当前可用库存。 */
    @Autowired
    private InventoryMapper inventoryMapper;

    @Override
    public List<MaterialRequest> findAll() {
        return materialRequestMapper.findAll();
    }

    @Override
    public MaterialRequest findById(Long id) {
        return materialRequestMapper.findById(id);
    }

    @Override
    public MaterialRequest findByRequestNo(String requestNo) {
        return materialRequestMapper.findByRequestNo(requestNo);
    }

    @Override
    public List<MaterialRequest> findByStatus(String requestStatus) {
        return materialRequestMapper.findByStatus(requestStatus);
    }

    @Override
    @Transactional
    public MaterialRequest create(MaterialRequest materialRequest) {
        validate(materialRequest);
        materialRequest.setRequestNo(generateRequestNo());
        materialRequest.setCreatedAt(new Date());
        materialRequest.setUpdatedAt(new Date());
        materialRequest.setIsDeleted(0);
        fillMaterialInfoAndStock(materialRequest, true);
        materialRequestMapper.insert(materialRequest);
        return materialRequestMapper.findById(materialRequest.getId());
    }

    @Override
    @Transactional
    public MaterialRequest update(MaterialRequest materialRequest) {
        MaterialRequest existing = materialRequestMapper.findById(materialRequest.getId());
        if (existing == null) {
            throw new RuntimeException("物料请求不存在");
        }
        if (materialRequest.getSourceTaskId() != null) existing.setSourceTaskId(materialRequest.getSourceTaskId());
        if (materialRequest.getSourceTaskNo() != null) existing.setSourceTaskNo(materialRequest.getSourceTaskNo());
        if (materialRequest.getProductName() != null) existing.setProductName(materialRequest.getProductName());
        if (materialRequest.getMaterialId() != null) existing.setMaterialId(materialRequest.getMaterialId());
        if (materialRequest.getRequiredQuantity() != null) existing.setRequiredQuantity(materialRequest.getRequiredQuantity());
        if (materialRequest.getWarehouseId() != null) existing.setWarehouseId(materialRequest.getWarehouseId());
        if (materialRequest.getRequestStatus() != null) existing.setRequestStatus(materialRequest.getRequestStatus());
        if (materialRequest.getRemark() != null) existing.setRemark(materialRequest.getRemark());
        existing.setUpdatedAt(new Date());
        validate(existing);
        fillMaterialInfoAndStock(existing, true);
        materialRequestMapper.update(existing);
        return materialRequestMapper.findById(existing.getId());
    }

    @Override
    @Transactional
    public MaterialRequest requestPurchase(Long id) {
        MaterialRequest existing = materialRequestMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("物料请求不存在");
        }
        fillMaterialInfoAndStock(existing, false);
        if (existing.getShortageQuantity() == null || existing.getShortageQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("当前库存充足，不需要发送采购请求");
        }
        existing.setRequestStatus(STATUS_PURCHASE_REQUESTED);
        existing.setUpdatedAt(new Date());
        materialRequestMapper.update(existing);
        return materialRequestMapper.findById(id);
    }

    @Override
    @Transactional
    public MaterialRequest markPurchaseOrdered(Long id) {
        MaterialRequest existing = materialRequestMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("物料请求不存在");
        }
        existing.setRequestStatus(STATUS_PURCHASE_ORDERED);
        existing.setUpdatedAt(new Date());
        materialRequestMapper.update(existing);
        return materialRequestMapper.findById(id);
    }

    @Override
    public void delete(Long id) {
        materialRequestMapper.delete(id);
    }

    private void validate(MaterialRequest materialRequest) {
        if (materialRequest.getMaterialId() == null) {
            throw new RuntimeException("需求物料不能为空");
        }
        if (materialRequest.getRequiredQuantity() == null || materialRequest.getRequiredQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("需求数量必须大于0");
        }
    }

    /**
     * 填充物料主数据和库存判断；create/update时根据库存自动确定 ENOUGH/SHORTAGE。
     */
    private void fillMaterialInfoAndStock(MaterialRequest materialRequest, boolean autoSetStatus) {
        Material material = materialMapper.findById(materialRequest.getMaterialId());
        if (material == null) {
            throw new RuntimeException("物料不存在");
        }
        materialRequest.setMaterialCode(material.getMaterialCode());
        materialRequest.setMaterialName(material.getMaterialName());

        BigDecimal availableQuantity = calculateAvailableQuantity(materialRequest.getMaterialId(), materialRequest.getWarehouseId());
        BigDecimal requiredQuantity = defaultZero(materialRequest.getRequiredQuantity());
        BigDecimal shortageQuantity = requiredQuantity.subtract(availableQuantity);
        if (shortageQuantity.compareTo(BigDecimal.ZERO) < 0) {
            shortageQuantity = BigDecimal.ZERO;
        }
        materialRequest.setAvailableQuantity(availableQuantity);
        materialRequest.setShortageQuantity(shortageQuantity);

        if (autoSetStatus) {
            materialRequest.setRequestStatus(shortageQuantity.compareTo(BigDecimal.ZERO) > 0 ? STATUS_SHORTAGE : STATUS_ENOUGH);
        }
    }

    private BigDecimal calculateAvailableQuantity(Long materialId, Long warehouseId) {
        if (warehouseId == null) {
            return defaultZero(inventoryMapper.sumQuantityByMaterialId(materialId));
        }
        List<Inventory> inventoryList = inventoryMapper.search(materialId, warehouseId, null);
        BigDecimal sum = BigDecimal.ZERO;
        for (Inventory inventory : inventoryList) {
            sum = sum.add(defaultZero(inventory.getQuantity()));
        }
        return sum;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String generateRequestNo() {
        return "MR-" + new SimpleDateFormat("yyyyMMdd-HHmmssSSS").format(new Date());
    }
}