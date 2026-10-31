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
import java.util.Objects;

@Service
public class MaterialRequestServiceImpl implements MaterialRequestService {

    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;
    @Autowired private com.mouse.mes.service.PurchaseOrderService purchaseOrders;
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
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        validate(materialRequest);
        if(materialRequest.getSourceTaskId()==null)throw new IllegalArgumentException("请选择来源生产任务");
        var tasks=db.queryForList("SELECT t.task_no,t.product_name,t.task_status FROM t_production_task t WHERE id=?",materialRequest.getSourceTaskId());
        if(tasks.isEmpty()||((Number)tasks.get(0).get("task_status")).intValue()>=4)throw new IllegalArgumentException("来源任务不存在或已结束");
        materialRequest.setSourceTaskNo(tasks.get(0).get("task_no").toString());materialRequest.setProductName(Objects.toString(tasks.get(0).get("product_name"),""));
        if(db.queryForObject("SELECT COUNT(*) FROM t_material_request WHERE source_task_id=? AND material_id=? AND COALESCE(warehouse_id,0)=? AND is_deleted=0 AND request_status IN ('PENDING_CHECK','ENOUGH','SHORTAGE','PURCHASE_REQUESTED')",Long.class,materialRequest.getSourceTaskId(),materialRequest.getMaterialId(),materialRequest.getWarehouseId()==null?0:materialRequest.getWarehouseId())>0)throw new IllegalArgumentException("该任务物料已有未处理请求，请查看原请求，勿重复登记");
        if(db.queryForObject("SELECT COUNT(*) FROM mm_task_context WHERE task_id=?",Long.class,materialRequest.getSourceTaskId())>0&&db.queryForObject("SELECT COUNT(*) FROM mm_task_context c JOIN mm_work_material m ON m.work_order_id=c.work_order_id WHERE c.task_id=? AND m.material_id=?",Long.class,materialRequest.getSourceTaskId(),materialRequest.getMaterialId())==0)throw new IllegalArgumentException("请选择该工单BOM中的物料");
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
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        MaterialRequest existing = materialRequestMapper.findById(materialRequest.getId());
        if (existing == null) {
            throw new RuntimeException("物料请求不存在");
        }
        if (STATUS_PURCHASE_REQUESTED.equals(existing.getRequestStatus()) || STATUS_PURCHASE_ORDERED.equals(existing.getRequestStatus()))
            throw new IllegalArgumentException("已发送采购的需求不可改写，请查看采购进度");
        if (materialRequest.getSourceTaskId() != null) existing.setSourceTaskId(materialRequest.getSourceTaskId());
        if (materialRequest.getSourceTaskNo() != null) existing.setSourceTaskNo(materialRequest.getSourceTaskNo());
        if (materialRequest.getProductName() != null) existing.setProductName(materialRequest.getProductName());
        if (materialRequest.getMaterialId() != null) existing.setMaterialId(materialRequest.getMaterialId());
        if (materialRequest.getRequiredQuantity() != null) existing.setRequiredQuantity(materialRequest.getRequiredQuantity());
        if (materialRequest.getWarehouseId() != null) existing.setWarehouseId(materialRequest.getWarehouseId());
        if (materialRequest.getRequestStatus() != null) existing.setRequestStatus(materialRequest.getRequestStatus());
        if(materialRequest.getRequiredDate()!=null)existing.setRequiredDate(materialRequest.getRequiredDate());
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
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        MaterialRequest existing = materialRequestMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("物料请求不存在");
        }
        if (STATUS_PURCHASE_ORDERED.equals(existing.getRequestStatus())) throw new IllegalArgumentException("该需求已转采购单，请查看关联订单");
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
        if (existing.getPurchaseOrderId() == null) throw new IllegalArgumentException("请通过生成采购订单完成转单，不能单独修改转单状态");
        existing.setRequestStatus(STATUS_PURCHASE_ORDERED);
        existing.setUpdatedAt(new Date());
        materialRequestMapper.update(existing);
        return materialRequestMapper.findById(id);
    }

    /** Lock before creating the order: provenance and status commit together, including retries. */
    @Override
    @Transactional
    public com.mouse.mes.entity.PurchaseOrder createPurchaseOrder(Long id, com.mouse.mes.dto.PurchaseOrderRequest input, Long actor) {
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        MaterialRequest source = materialRequestMapper.findById(id);
        if (source == null) throw new IllegalArgumentException("物料请求不存在");
        if (source.getPurchaseOrderId() != null) return purchaseOrders.findById(source.getPurchaseOrderId());
        if (!STATUS_PURCHASE_REQUESTED.equals(source.getRequestStatus())) throw new IllegalArgumentException("请先由仓库核对缺口并发送采购请求");
        fillMaterialInfoAndStock(source, false);
        if (source.getShortageQuantity().signum() <= 0) throw new IllegalArgumentException("当前库存已充足，请重新核对需求，无需采购");
        var lines = input.getItems();
        if (lines == null || lines.size() != 1 || lines.get(0).getUnitPrice() == null || lines.get(0).getUnitPrice().signum() < 0)
            throw new IllegalArgumentException("请填写有效的采购单价");
        var line = new com.mouse.mes.entity.PurchaseOrderItem();
        line.setMaterialId(source.getMaterialId());
        line.setQuantity(source.getShortageQuantity());
        line.setUnitPrice(lines.get(0).getUnitPrice());
        line.setAmount(line.getQuantity().multiply(line.getUnitPrice()));
        var order = new com.mouse.mes.dto.PurchaseOrderRequest();
        order.setPoNo("PO-MR-" + id);
        order.setSupplierId(input.getSupplierId());
        order.setOrderDate(new Date());
        order.setExpectedDate(input.getExpectedDate());
        order.setStatus("DRAFT");
        order.setCreatedBy(actor);
        order.setRemark("来源物料请求 " + source.getRequestNo() + "；" + Objects.toString(input.getRemark(), ""));
        order.setItems(List.of(line));
        var created = purchaseOrders.create(order);
        db.update("INSERT INTO mm_purchase_request_order(request_id,po_id) VALUES(?,?)", id, created.getId());
        source.setRequestStatus(STATUS_PURCHASE_ORDERED);
        source.setUpdatedAt(new Date());
        materialRequestMapper.update(source);
        return created;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        db.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        MaterialRequest existing = materialRequestMapper.findById(id);
        if (existing != null && (STATUS_PURCHASE_REQUESTED.equals(existing.getRequestStatus()) || STATUS_PURCHASE_ORDERED.equals(existing.getRequestStatus())))
            throw new IllegalArgumentException("已发送采购的需求不能删除，须保留来源记录");
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
