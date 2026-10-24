package com.mouse.mes.impl;

import com.mouse.mes.constant.PurchaseOrderStatus;
import com.mouse.mes.dto.PurchaseOrderRequest;
import com.mouse.mes.dto.PurchaseReceiveRequest;
import com.mouse.mes.entity.PurchaseOrder;
import com.mouse.mes.entity.PurchaseOrderItem;
import com.mouse.mes.entity.SalesOrder;
import com.mouse.mes.entity.StockIn;
import com.mouse.mes.entity.StockInItem;
import com.mouse.mes.entity.Supplier;
import com.mouse.mes.mapper.PurchaseOrderItemMapper;
import com.mouse.mes.mapper.PurchaseOrderMapper;
import com.mouse.mes.mapper.SalesOrderMapper;
import com.mouse.mes.mapper.StockInItemMapper;
import com.mouse.mes.mapper.StockInMapper;
import com.mouse.mes.mapper.SupplierMapper;
import com.mouse.mes.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Service
public class PurchaseOrderServiceImpl implements PurchaseOrderService {
    @Autowired private org.springframework.jdbc.core.JdbcTemplate ledgerDb;

    /**
     * 采购订单主表数据库访问对象。
     */
    @Autowired
    private PurchaseOrderMapper purchaseOrderMapper;

    /**
     * 采购订单明细数据库访问对象。
     */
    @Autowired
    private PurchaseOrderItemMapper purchaseOrderItemMapper;

    /**
     * 供应商数据库访问对象，用于校验供应商是否存在且启用。
     */
    @Autowired
    private SupplierMapper supplierMapper;

    /**
     * 销售订单数据库访问对象，用于接受销售订单时查询订单信息。
     */
    @Autowired
    private SalesOrderMapper salesOrderMapper;

    /**
     * 入库单数据库访问对象，采购到货时生成入库单。
     */
    @Autowired
    private StockInMapper stockInMapper;

    /**
     * 入库单明细数据库访问对象，采购到货时生成入库明细。
     */
    @Autowired
    private StockInItemMapper stockInItemMapper;

    /**
     * 查询全部未删除采购订单，并带出每个订单的采购明细。
     */
    @Override
    public List<PurchaseOrder> findAll() {
        List<PurchaseOrder> orders = purchaseOrderMapper.findAll();
        for (PurchaseOrder order : orders) {
            fillItems(order);
        }
        return orders;
    }

    /**
     * 按采购状态查询采购订单，并带出每个订单的采购明细。
     */
    @Override
    public List<PurchaseOrder> findByStatus(String status) {
        List<PurchaseOrder> orders = purchaseOrderMapper.findByStatus(status);
        for (PurchaseOrder order : orders) {
            fillItems(order);
        }
        return orders;
    }

    /**
     * 按采购订单ID查询采购订单，并带出采购明细。
     */
    @Override
    public PurchaseOrder findById(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderMapper.findById(id);
        fillItems(purchaseOrder);
        return purchaseOrder;
    }

    /**
     * 按采购订单编号查询采购订单，并带出采购明细。
     */
    @Override
    public PurchaseOrder findByPoNo(String poNo) {
        PurchaseOrder purchaseOrder = purchaseOrderMapper.findByPoNo(poNo);
        fillItems(purchaseOrder);
        return purchaseOrder;
    }

    /**
     * 兼容旧接口的采购订单创建方法。
     */
    @Override
    @Transactional
    public PurchaseOrder create(PurchaseOrder purchaseOrder) {
        PurchaseOrderRequest request = new PurchaseOrderRequest();
        request.setPoNo(purchaseOrder.getPoNo());
        request.setSupplierId(purchaseOrder.getSupplierId());
        request.setOrderDate(purchaseOrder.getOrderDate());
        request.setExpectedDate(purchaseOrder.getExpectedDate());
        request.setTotalAmount(purchaseOrder.getTotalAmount());
        request.setStatus(purchaseOrder.getStatus());
        request.setRemark(purchaseOrder.getRemark());
        request.setCreatedBy(purchaseOrder.getCreatedBy());
        request.setItems(purchaseOrder.getItems());
        return create(request);
    }

    /**
     * 新增采购订单，同时保存一个或多个采购订单明细。
     */
    @Override
    @Transactional
    public PurchaseOrder create(PurchaseOrderRequest request) {
        ledgerDb.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        PurchaseOrder purchaseOrder = buildPurchaseOrder(request);
        if (purchaseOrder.getStatus() == null || purchaseOrder.getStatus().isBlank()) {
            purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT);
        }
        if(!PurchaseOrderStatus.DRAFT.equals(purchaseOrder.getStatus())) throw new IllegalArgumentException("新采购单必须从草稿开始，再提交审批");
        validateNewOrder(purchaseOrder);
        validateItemsNotEmpty(request.getItems());
        purchaseOrder.setTotalAmount(resolveTotalAmount(purchaseOrder.getTotalAmount(), request.getItems()));
        purchaseOrder.setCreatedAt(new Date());
        purchaseOrder.setUpdatedAt(new Date());
        purchaseOrder.setIsDeleted(0);
        purchaseOrderMapper.insert(purchaseOrder);
        saveItems(purchaseOrder.getId(), request.getItems());
        return findById(purchaseOrder.getId());
    }

    /**
     * 兼容旧接口的采购订单修改方法。
     */
    @Override
    @Transactional
    public PurchaseOrder update(PurchaseOrder purchaseOrder) {
        PurchaseOrderRequest request = new PurchaseOrderRequest();
        request.setPoNo(purchaseOrder.getPoNo());
        request.setSupplierId(purchaseOrder.getSupplierId());
        request.setOrderDate(purchaseOrder.getOrderDate());
        request.setExpectedDate(purchaseOrder.getExpectedDate());
        request.setTotalAmount(purchaseOrder.getTotalAmount());
        request.setStatus(purchaseOrder.getStatus());
        request.setRemark(purchaseOrder.getRemark());
        request.setCreatedBy(purchaseOrder.getCreatedBy());
        request.setItems(purchaseOrder.getItems());
        return update(purchaseOrder.getId(), request);
    }

    /**
     * 修改采购订单；如果请求中携带明细，则整体替换旧明细。
     */
    @Override
    @Transactional
    public PurchaseOrder update(Long id, PurchaseOrderRequest request) {
        ledgerDb.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        PurchaseOrder existing = requireOrder(id);
        ensureEditable(existing);
        if(request.getStatus()!=null&&!request.getStatus().equals(existing.getStatus())) throw new IllegalArgumentException("状态请通过提交、审批、收货操作推进");
        PurchaseOrder purchaseOrder = buildPurchaseOrder(request);
        mergeMissingFields(purchaseOrder, existing);
        validateOrderBase(purchaseOrder);
        if (!PurchaseOrderStatus.isValid(purchaseOrder.getStatus())) {
            throw new IllegalArgumentException("采购订单状态不合法");
        }
        if (request.getItems() != null) {
            validateItemsNotEmpty(request.getItems());
            purchaseOrder.setTotalAmount(resolveTotalAmount(purchaseOrder.getTotalAmount(), request.getItems()));
        }
        purchaseOrder.setId(id);
        purchaseOrder.setCreatedAt(existing.getCreatedAt());
        purchaseOrder.setUpdatedAt(new Date());
        purchaseOrder.setIsDeleted(existing.getIsDeleted());
        purchaseOrderMapper.update(purchaseOrder);
        if (request.getItems() != null) {
            purchaseOrderItemMapper.deleteByPoId(id);
            saveItems(id, request.getItems());
        }
        return findById(id);
    }

    /**
     * 提交采购订单，状态只能从 DRAFT 流转为 SUBMITTED。
     */
    @Override
    @Transactional
    public PurchaseOrder submit(Long id) {
        ledgerDb.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        PurchaseOrder purchaseOrder = requireOrder(id);
        requireStatus(purchaseOrder, PurchaseOrderStatus.DRAFT, "只有草稿采购单可以提交");
        purchaseOrder.setStatus(PurchaseOrderStatus.SUBMITTED);
        purchaseOrder.setUpdatedAt(new Date());
        purchaseOrderMapper.update(purchaseOrder);
        return findById(id);
    }

    /**
     * 审核采购订单，状态只能从 SUBMITTED 流转为 APPROVED。
     */
    @Override
    @Transactional
    public PurchaseOrder approve(Long id) {
        ledgerDb.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        PurchaseOrder purchaseOrder = requireOrder(id);
        requireStatus(purchaseOrder, PurchaseOrderStatus.SUBMITTED, "只有已提交采购单可以审核");
        purchaseOrder.setStatus(PurchaseOrderStatus.APPROVED);
        purchaseOrder.setUpdatedAt(new Date());
        purchaseOrderMapper.update(purchaseOrder);
        return findById(id);
    }

    /**
     * 取消采购订单，已完成或已取消的订单不能再次取消。
     */
    @Override
    @Transactional
    public PurchaseOrder cancel(Long id) {
        ledgerDb.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        PurchaseOrder purchaseOrder = requireOrder(id);
        if (PurchaseOrderStatus.COMPLETED.equals(purchaseOrder.getStatus())) {
            throw new IllegalArgumentException("已完成采购单不能取消");
        }
        if (PurchaseOrderStatus.CANCELLED.equals(purchaseOrder.getStatus())) {
            throw new IllegalArgumentException("采购单已取消");
        }
        purchaseOrder.setStatus(PurchaseOrderStatus.CANCELLED);
        purchaseOrder.setUpdatedAt(new Date());
        purchaseOrderMapper.update(purchaseOrder);
        return findById(id);
    }

    /**
     * 采购到货，校验到货数量，生成采购入库单，并刷新采购订单状态。
     */
    @Override
    @Transactional
    public PurchaseOrder receive(Long id, PurchaseReceiveRequest request) {
        throw new IllegalArgumentException("请在采购收货页面按批次登记、检验放行并过账，避免数量与库存不同步");
    }

    /**
     * 逻辑删除采购订单，删除后查询列表不再返回该订单。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        ledgerDb.queryForList("SELECT id FROM mm_guard WHERE id=1 FOR UPDATE");
        PurchaseOrder purchaseOrder = purchaseOrderMapper.findById(id);
        if (purchaseOrder != null) {
            if(!PurchaseOrderStatus.DRAFT.equals(purchaseOrder.getStatus()))throw new IllegalArgumentException("仅未提交草稿可删除，已审批和已收货单据须保留");
            purchaseOrder.setIsDeleted(1);
            purchaseOrder.setUpdatedAt(new Date());
            purchaseOrderMapper.update(purchaseOrder);
        }
    }

    /**
     * 接受销售订单并自动创建采购单。
     */
    @Override
    @Transactional
    public PurchaseOrder createFromSalesOrder(Long salesOrderId) {
        return createFromSalesOrder(salesOrderId, 1L, null);
    }

    /**
     * 接受销售订单并自动创建采购单（带供应商和预计到货日期）。
     */
    @Override
    @Transactional
    public PurchaseOrder createFromSalesOrder(Long salesOrderId, Long supplierId, String expectedDate) {
        SalesOrder salesOrder = salesOrderMapper.findById(salesOrderId);
        if (salesOrder == null) {
            throw new IllegalArgumentException("销售订单不存在");
        }
        if (!"CONFIRMED".equals(salesOrder.getStatus())) {
            throw new IllegalArgumentException("只有已确认的销售订单才能创建采购单");
        }

        String productName = salesOrder.getSoNo();
        String materialName = "";
        if (productName.contains("A类")) {
            materialName = "A类零件";
        } else if (productName.contains("B类")) {
            materialName = "B类零件";
        } else if (productName.contains("C类")) {
            materialName = "C类零件";
        } else {
            materialName = "其他零件";
        }

        String poNo = "PO-" + salesOrder.getSoNo();

        PurchaseOrder existing = purchaseOrderMapper.findByPoNoIncludingDeleted(poNo);
        if (existing != null && existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
            existing.setIsDeleted(0);
            existing.setSupplierId(supplierId != null ? supplierId : 1L);
            existing.setSalesOrderId(salesOrderId);
            existing.setOrderDate(new Date());
            if (expectedDate != null && !expectedDate.isBlank()) {
                try {
                    existing.setExpectedDate(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(expectedDate));
                } catch (Exception e) {
                    throw new IllegalArgumentException("预计到货日期格式不正确");
                }
            }
            existing.setTotalAmount(salesOrder.getTotalAmount());
            existing.setStatus(PurchaseOrderStatus.SUBMITTED);
            existing.setUpdatedAt(new Date());
            purchaseOrderMapper.update(existing);

            purchaseOrderItemMapper.deleteByPoId(existing.getId());
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPoId(existing.getId());
            item.setMaterialId(salesOrder.getProductId() != null ? salesOrder.getProductId() : 1L);
            item.setQuantity(BigDecimal.valueOf(salesOrder.getQuantity() != null ? salesOrder.getQuantity() : 0));
            item.setUnitPrice(salesOrder.getUnitPrice() != null ? salesOrder.getUnitPrice() : BigDecimal.ZERO);
            item.setAmount(salesOrder.getTotalAmount() != null ? salesOrder.getTotalAmount() : BigDecimal.ZERO);
            item.setReceivedQuantity(BigDecimal.ZERO);
            purchaseOrderItemMapper.insert(item);

            salesOrder.setMaterialStatus("PURCHASING");
            salesOrder.setUpdatedAt(new Date());
            salesOrderMapper.update(salesOrder);

            return findById(existing.getId());
        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setPoNo(poNo);
        purchaseOrder.setSupplierId(supplierId != null ? supplierId : 1L);
        purchaseOrder.setSalesOrderId(salesOrderId);
        purchaseOrder.setOrderDate(new Date());
        if (expectedDate != null && !expectedDate.isBlank()) {
            try {
                purchaseOrder.setExpectedDate(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(expectedDate));
            } catch (Exception e) {
                throw new IllegalArgumentException("预计到货日期格式不正确");
            }
        }
        purchaseOrder.setTotalAmount(salesOrder.getTotalAmount());
        purchaseOrder.setStatus(PurchaseOrderStatus.SUBMITTED);
        purchaseOrder.setCreatedAt(new Date());
        purchaseOrder.setUpdatedAt(new Date());
        purchaseOrder.setIsDeleted(0);

        purchaseOrderMapper.insert(purchaseOrder);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPoId(purchaseOrder.getId());
        item.setMaterialId(salesOrder.getProductId() != null ? salesOrder.getProductId() : 1L);
        item.setQuantity(BigDecimal.valueOf(salesOrder.getQuantity() != null ? salesOrder.getQuantity() : 0));
        item.setUnitPrice(salesOrder.getUnitPrice() != null ? salesOrder.getUnitPrice() : BigDecimal.ZERO);
        item.setAmount(salesOrder.getTotalAmount() != null ? salesOrder.getTotalAmount() : BigDecimal.ZERO);
        item.setReceivedQuantity(BigDecimal.ZERO);
        purchaseOrderItemMapper.insert(item);

        salesOrder.setMaterialStatus("PURCHASING");
        salesOrder.setUpdatedAt(new Date());
        salesOrderMapper.update(salesOrder);

        return findById(purchaseOrder.getId());
    }

    /**
     * 给采购订单填充采购明细列表。
     */
    private void fillItems(PurchaseOrder purchaseOrder) {
        if (purchaseOrder != null) {
            purchaseOrder.setItems(purchaseOrderItemMapper.findByPoId(purchaseOrder.getId()));
        }
    }

    /**
     * 将前端请求对象转换为采购订单实体。
     */
    private PurchaseOrder buildPurchaseOrder(PurchaseOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("采购订单不能为空");
        }
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setPoNo(request.getPoNo());
        purchaseOrder.setSupplierId(request.getSupplierId());
        purchaseOrder.setOrderDate(request.getOrderDate());
        purchaseOrder.setExpectedDate(request.getExpectedDate());
        purchaseOrder.setTotalAmount(request.getTotalAmount());
        purchaseOrder.setStatus(request.getStatus());
        purchaseOrder.setRemark(request.getRemark());
        purchaseOrder.setCreatedBy(request.getCreatedBy());
        return purchaseOrder;
    }

    /**
     * 修改订单时，将请求中未传的字段用原订单字段补齐，避免被空值覆盖。
     */
    private void mergeMissingFields(PurchaseOrder target, PurchaseOrder existing) {
        if (target.getPoNo() == null || target.getPoNo().isBlank()) {
            target.setPoNo(existing.getPoNo());
        }
        if (target.getSupplierId() == null) {
            target.setSupplierId(existing.getSupplierId());
        }
        if (target.getOrderDate() == null) {
            target.setOrderDate(existing.getOrderDate());
        }
        if (target.getExpectedDate() == null) {
            target.setExpectedDate(existing.getExpectedDate());
        }
        if (target.getTotalAmount() == null) {
            target.setTotalAmount(existing.getTotalAmount());
        }
        if (target.getStatus() == null || target.getStatus().isBlank()) {
            target.setStatus(existing.getStatus());
        }
        if (target.getCreatedBy() == null) {
            target.setCreatedBy(existing.getCreatedBy());
        }
    }

    /**
     * 校验新增采购订单的基础信息和订单编号唯一性。
     */
    private void validateNewOrder(PurchaseOrder purchaseOrder) {
        validateOrderBase(purchaseOrder);
        if (!PurchaseOrderStatus.isValid(purchaseOrder.getStatus())) {
            throw new IllegalArgumentException("采购订单状态不合法");
        }
        if (purchaseOrderMapper.findByPoNo(purchaseOrder.getPoNo()) != null) {
            throw new IllegalArgumentException("采购订单编号已存在");
        }
    }

    /**
     * 校验采购订单主表基础字段。
     */
    private void validateOrderBase(PurchaseOrder purchaseOrder) {
        if (purchaseOrder.getPoNo() == null || purchaseOrder.getPoNo().isBlank()) {
            throw new IllegalArgumentException("订单编号不能为空");
        }
        if (purchaseOrder.getSupplierId() == null) {
            throw new IllegalArgumentException("供应商ID不能为空");
        }
        Supplier supplier = supplierMapper.findById(purchaseOrder.getSupplierId());
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        if (supplier.getStatus() == null || supplier.getStatus() != 1) {
            throw new IllegalArgumentException("供应商已禁用，不能创建采购订单");
        }
    }

    /**
     * 校验采购订单明细不能为空。
     */
    private void validateItemsNotEmpty(List<PurchaseOrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("采购订单明细不能为空");
        }
    }

    /**
     * 保存采购订单明细。
     */
    private void saveItems(Long purchaseOrderId, List<PurchaseOrderItem> items) {
        for (PurchaseOrderItem item : items) {
            validateItem(item);
            item.setPoId(purchaseOrderId);
            if (item.getUnitPrice() == null) {
                item.setUnitPrice(BigDecimal.ZERO);
            }
            if (item.getAmount() == null) {
                item.setAmount(item.getUnitPrice().multiply(item.getQuantity()));
            }
            item.setReceivedQuantity(BigDecimal.ZERO);
            purchaseOrderItemMapper.insert(item);
        }
    }

    /**
     * 校验单行采购明细字段。
     */
    private void validateItem(PurchaseOrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("采购明细不能为空");
        }
        if (item.getMaterialId() == null) {
            throw new IllegalArgumentException("物料ID不能为空");
        }
        if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("采购数量必须大于0");
        }
    }

    /**
     * 如果前端没有传订单总金额，则根据采购明细自动计算总金额。
     */
    private BigDecimal resolveTotalAmount(BigDecimal totalAmount, List<PurchaseOrderItem> items) {
        if (totalAmount != null) {
            return totalAmount;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem item : items) {
            validateItem(item);
            BigDecimal unitPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
            total = total.add(unitPrice.multiply(item.getQuantity()));
        }
        return total;
    }

    /**
     * 查询采购订单；不存在时抛出业务异常。
     */
    private PurchaseOrder requireOrder(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderMapper.findById(id);
        if (purchaseOrder == null) {
            throw new IllegalArgumentException("采购订单不存在");
        }
        return purchaseOrder;
    }

    /**
     * 校验采购订单是否仍允许修改。
     */
    private void ensureEditable(PurchaseOrder purchaseOrder) {
        if (!PurchaseOrderStatus.DRAFT.equals(purchaseOrder.getStatus())) {
            throw new IllegalArgumentException("当前状态不允许修改采购订单");
        }
    }

    /**
     * 校验采购订单当前状态是否等于期望状态。
     */
    private void requireStatus(PurchaseOrder purchaseOrder, String expectedStatus, String message) {
        if (!expectedStatus.equals(purchaseOrder.getStatus())) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 校验采购订单当前是否允许到货。
     */
    private void validateReceiveOrderStatus(PurchaseOrder purchaseOrder) {
        if (PurchaseOrderStatus.CANCELLED.equals(purchaseOrder.getStatus())) {
            throw new IllegalArgumentException("已取消采购单不能到货");
        }
        if (PurchaseOrderStatus.COMPLETED.equals(purchaseOrder.getStatus())) {
            throw new IllegalArgumentException("已完成采购单不能重复到货");
        }
        if (!PurchaseOrderStatus.APPROVED.equals(purchaseOrder.getStatus())
                && !PurchaseOrderStatus.PARTIAL_RECEIVED.equals(purchaseOrder.getStatus())) {
            throw new IllegalArgumentException("只有已审核或部分到货采购单可以到货");
        }
    }

    /**
     * 校验采购到货请求基础字段。
     */
    private void validateReceiveRequest(PurchaseReceiveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("到货请求不能为空");
        }
        if (request.getWarehouseId() == null) {
            throw new IllegalArgumentException("仓库ID不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("到货明细不能为空");
        }
    }

    /**
     * 创建采购到货对应的入库单主表。
     */
    private StockIn createStockIn(PurchaseOrder purchaseOrder, PurchaseReceiveRequest request) {
        StockIn stockIn = new StockIn();
        stockIn.setStockInNo(nextStockInNo(purchaseOrder.getPoNo()));
        stockIn.setStockInType("PURCHASE");
        stockIn.setWarehouseId(request.getWarehouseId());
        stockIn.setSourceOrderId(purchaseOrder.getId());
        stockIn.setSourceOrderNo(purchaseOrder.getPoNo());
        stockIn.setTotalQuantity(BigDecimal.ZERO);
        stockIn.setStatus("PENDING");
        stockIn.setRemark(request.getRemark());
        stockIn.setCreatedBy(request.getCreatedBy());
        stockIn.setCreatedAt(new Date());
        stockIn.setUpdatedAt(new Date());
        stockIn.setIsDeleted(0);
        return stockIn;
    }

    /**
     * 校验单行采购到货明细，防止明细不存在、数量非法或超量到货。
     */
    private void validateReceiveItem(PurchaseOrder purchaseOrder, PurchaseOrderItem orderItem,
                                     PurchaseReceiveRequest.Item receiveItem) {
        if (receiveItem == null) {
            throw new IllegalArgumentException("到货明细不能为空");
        }
        if (orderItem == null || !purchaseOrder.getId().equals(orderItem.getPoId())) {
            throw new IllegalArgumentException("采购明细不存在");
        }
        if (receiveItem.getReceiveQuantity() == null || receiveItem.getReceiveQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("到货数量必须大于0");
        }
        BigDecimal remainingQuantity = orderItem.getQuantity().subtract(safe(orderItem.getReceivedQuantity()));
        if (receiveItem.getReceiveQuantity().compareTo(remainingQuantity) > 0) {
            throw new IllegalArgumentException("到货数量不能超过采购数量");
        }
    }

    /**
     * 创建采购到货对应的入库单明细。
     */
    private void createStockInItem(Long stockInId, PurchaseOrderItem orderItem, PurchaseReceiveRequest.Item receiveItem) {
        StockInItem stockInItem = new StockInItem();
        stockInItem.setStockInId(stockInId);
        stockInItem.setMaterialId(orderItem.getMaterialId());
        stockInItem.setLotId(receiveItem.getLotId());
        stockInItem.setQuantity(receiveItem.getReceiveQuantity());
        stockInItem.setLocationId(receiveItem.getLocationId());
        stockInItem.setRemark(receiveItem.getRemark());
        stockInItemMapper.insert(stockInItem);
    }

    /**
     * 根据所有采购明细的累计到货数量刷新采购订单状态。
     */
    private void updateReceiveStatus(Long purchaseOrderId) {
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.findByPoId(purchaseOrderId);
        boolean allReceived = !items.isEmpty() && items.stream()
                .allMatch(item -> safe(item.getReceivedQuantity()).compareTo(item.getQuantity()) >= 0);
        PurchaseOrder purchaseOrder = purchaseOrderMapper.findById(purchaseOrderId);
        purchaseOrder.setStatus(allReceived ? PurchaseOrderStatus.COMPLETED : PurchaseOrderStatus.PARTIAL_RECEIVED);
        purchaseOrder.setUpdatedAt(new Date());
        purchaseOrderMapper.update(purchaseOrder);

        if (allReceived && purchaseOrder.getSalesOrderId() != null) {
            SalesOrder salesOrder = salesOrderMapper.findById(purchaseOrder.getSalesOrderId());
            if (salesOrder != null) {
                salesOrder.setMaterialStatus("PROCURED");
                salesOrder.setUpdatedAt(new Date());
                salesOrderMapper.update(salesOrder);
            }
        }
    }

    /**
     * 生成采购到货入库单号。
     */
    private String nextStockInNo(String poNo) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return "SI-" + poNo + "-" + timestamp;
    }

    /**
     * 将可能为空的数量转换为零，方便做数量计算。
     */
    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
