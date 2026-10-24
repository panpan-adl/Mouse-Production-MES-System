package com.mouse.mes.impl;

import com.mouse.mes.entity.ProductionPlan;
import com.mouse.mes.entity.ProductionReport;
import com.mouse.mes.entity.ProductionTask;
import com.mouse.mes.mapper.OrderMapper;
import com.mouse.mes.mapper.ProductionPlanMapper;
import com.mouse.mes.mapper.ProductionReportMapper;
import com.mouse.mes.mapper.ProductionTaskMapper;
import com.mouse.mes.service.ProductionReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ProductionReportServiceImpl implements ProductionReportService {

    @Autowired
    private com.mouse.mes.service.FoundationService foundationService;

    /** 销售订单状态：生产中。 */
    private static final int ORDER_STATUS_PRODUCING = 2;

    /** 销售订单状态：已完成。 */
    private static final int ORDER_STATUS_COMPLETED = 3;

    /** 生产计划状态：进行中。 */
    private static final int PLAN_STATUS_RUNNING = 1;

    /** 生产计划状态：已完成。 */
    private static final int PLAN_STATUS_COMPLETED = 2;

    /** 生产报工数据访问对象。 */
    @Autowired
    private ProductionReportMapper productionReportMapper;

    /** 生产任务数据访问对象，用于根据报工数量同步任务状态。 */
    @Autowired
    private ProductionTaskMapper productionTaskMapper;

    /** 生产计划数据访问对象，用于同步计划完成数量。 */
    @Autowired
    private ProductionPlanMapper productionPlanMapper;

    /** 销售订单数据访问对象，用于同步来源订单状态。 */
    @Autowired
    private OrderMapper orderMapper;

    @Override
    public List<ProductionReport> findAll() {
        return productionReportMapper.findAll();
    }

    @Override
    public ProductionReport findById(Long id) {
        return productionReportMapper.findById(id);
    }

    @Override
    public List<ProductionReport> findByTaskId(Long taskId) {
        return productionReportMapper.findByTaskId(taskId);
    }

    /**
     * 新增生产报工记录；这里只记录实际完成数量，合格和不合格数量交给质检模块判定。
     */
    @Override
    @Transactional
    public ProductionReport create(ProductionReport productionReport) {
        foundationService.checkTaskPredecessors(productionReport.getTaskId() == null ? 0 : productionReport.getTaskId());
        validateReportQuantity(productionReport.getReportQuantity());
        if (productionReport.getTaskId() == null) {
            throw new RuntimeException("生产任务不能为空");
        }
        foundationService.lockTask(productionReport.getTaskId());
        ProductionTask task = productionTaskMapper.findById(productionReport.getTaskId());
        if (task == null) {
            throw new RuntimeException("生产任务不存在");
        }
        if (!java.util.Objects.equals(task.getAssignedUserId(),productionReport.getReportUserId()) || task.getTaskStatus()==null || (task.getTaskStatus()!=1 && task.getTaskStatus()!=2)) throw new IllegalArgumentException("只有任务负责人可为已派发或生产中的任务报工");
        productionReport.setQualifiedQuantity(0);productionReport.setDefectQuantity(0);
        validateTotalQuantity(task, null, productionReport.getReportQuantity());
        fillDefaultReportFields(productionReport);
        productionReportMapper.insert(productionReport);
        syncTaskPlanAndOrder(productionReport.getTaskId());
        return productionReport;
    }

    /**
     * 修改生产报工记录；修改数量后会重新计算任务、计划和订单状态。
     */
    @Override
    @Transactional
    public ProductionReport update(Long id, ProductionReport productionReport) {
        ProductionReport existing = productionReportMapper.findById(id);
        if(existing!=null) foundationService.protectTask(existing.getTaskId());
        if (existing == null) {
            throw new RuntimeException("报工记录不存在");
        }
        if (productionReport.getReportQuantity() != null) {
            validateReportQuantity(productionReport.getReportQuantity());
            ProductionTask task = productionTaskMapper.findById(existing.getTaskId());
            if (task == null) {
                throw new RuntimeException("生产任务不存在");
            }
            validateTotalQuantity(task, existing.getId(), productionReport.getReportQuantity());
            existing.setReportQuantity(productionReport.getReportQuantity());
        }
        if (productionReport.getReportUserId() != null) {
            existing.setReportUserId(productionReport.getReportUserId());
        }
        if (productionReport.getReportUserName() != null && !productionReport.getReportUserName().trim().isEmpty()) {
            existing.setReportUserName(productionReport.getReportUserName().trim());
        }
        if (productionReport.getQualifiedQuantity() != null) {
            existing.setQualifiedQuantity(productionReport.getQualifiedQuantity());
        }
        if (productionReport.getDefectQuantity() != null) {
            existing.setDefectQuantity(productionReport.getDefectQuantity());
        }
        if (productionReport.getRemark() != null) {
            existing.setRemark(productionReport.getRemark());
        }
        productionReportMapper.update(existing);
        syncTaskPlanAndOrder(existing.getTaskId());
        return productionReportMapper.findById(id);
    }

    @Override
    public List<ProductionReport> findByReportUserId(Long reportUserId) {
        return productionReportMapper.findByReportUserId(reportUserId);
    }

    @Override
    public ProductionReport getTaskSummary(Long taskId) {
        return productionReportMapper.getTaskSummary(taskId);
    }

    /** 校验单次报工数量必须大于 0。 */
    private void validateReportQuantity(Integer reportQuantity) {
        if (reportQuantity == null || reportQuantity <= 0) {
            throw new RuntimeException("本次完成数量必须大于0");
        }
    }

    /** 校验累计报工数量不能超过生产任务数量。 */
    private void validateTotalQuantity(ProductionTask task, Long currentReportId, Integer newQuantity) {
        int otherReportedQuantity = 0;
        List<ProductionReport> reports = productionReportMapper.findByTaskId(task.getId());
        for (ProductionReport report : reports) {
            if (currentReportId != null && currentReportId.equals(report.getId())) {
                continue;
            }
            otherReportedQuantity += safeQuantity(report.getReportQuantity());
        }
        if (task.getQuantity() == null || otherReportedQuantity + safeQuantity(newQuantity) > task.getQuantity()) {
            throw new RuntimeException("累计报工数量不能超过任务数量");
        }
    }

    /** 填充报工记录默认字段。 */
    private void fillDefaultReportFields(ProductionReport productionReport) {
        if (productionReport.getQualifiedQuantity() == null) {
            productionReport.setQualifiedQuantity(0);
        }
        if (productionReport.getDefectQuantity() == null) {
            productionReport.setDefectQuantity(0);
        }
        if (productionReport.getReportTime() == null) {
            productionReport.setReportTime(new Date());
        }
    }

    /** 根据报工合计同步生产任务、生产计划和来源销售订单状态。 */
    private void syncTaskPlanAndOrder(Long taskId) {
        ProductionTask task = productionTaskMapper.findById(taskId);
        if (task == null) {
            return;
        }
        int completedQuantity = getTaskReportedQuantity(taskId);
        if (completedQuantity >= safeQuantity(task.getQuantity())) {
            task.setTaskStatus(ProductionTask.STATUS_PENDING_QC);
            if (task.getEndTime() == null) {
                task.setEndTime(new Date());
            }
        } else if (completedQuantity > 0 && task.getTaskStatus() != null && task.getTaskStatus() < ProductionTask.STATUS_PENDING_QC) {
            task.setTaskStatus(ProductionTask.STATUS_IN_PROGRESS);
            if (task.getStartTime() == null) {
                task.setStartTime(new Date());
            }
            task.setEndTime(null);
        }
        task.setUpdateTime(new Date());
        productionTaskMapper.update(task);
        syncPlanAndOrderAfterTaskProgress(task.getPlanId());
    }

    /** 获取某个任务已经累计报工的数量。 */
    private int getTaskReportedQuantity(Long taskId) {
        ProductionReport summary = productionReportMapper.getTaskSummary(taskId);
        return summary == null ? 0 : safeQuantity(summary.getReportQuantity());
    }

    /** 汇总同一生产计划下的任务状态，全部工序达到待质检后将计划和订单推进到完成。 */
    private void syncPlanAndOrderAfterTaskProgress(Long planId) {
        foundationService.syncProgress(planId);
    }

    /** 计划数量为空时按 1 处理，避免空计划被误判为完成。 */
    private int safePlanQuantity(Integer quantity) {
        return quantity == null || quantity <= 0 ? 1 : quantity;
    }

    /** 将空数量按 0 处理。 */
    private int safeQuantity(Integer quantity) {
        return quantity == null ? 0 : quantity;
    }
}
