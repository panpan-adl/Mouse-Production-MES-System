package com.mouse.mes.impl;

import com.mouse.mes.entity.ProductionPlan;
import com.mouse.mes.entity.ProductionReport;
import com.mouse.mes.entity.ProductionTask;
import com.mouse.mes.mapper.OrderMapper;
import com.mouse.mes.mapper.ProductionPlanMapper;
import com.mouse.mes.mapper.ProductionReportMapper;
import com.mouse.mes.mapper.ProductionTaskMapper;
import com.mouse.mes.service.ProductionTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ProductionTaskServiceImpl implements ProductionTaskService {

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

    /** 生产任务数据访问对象。 */
    @Autowired
    private ProductionTaskMapper productionTaskMapper;

    /** 生产计划数据访问对象，用于查询来源计划并同步计划进度。 */
    @Autowired
    private ProductionPlanMapper productionPlanMapper;

    /** 销售订单数据访问对象，用于同步来源订单状态。 */
    @Autowired
    private OrderMapper orderMapper;

    /** 生产报工数据访问对象，用于保存和汇总实际完成数量。 */
    @Autowired
    private ProductionReportMapper productionReportMapper;

    @Override
    public List<ProductionTask> findAll() {
        return productionTaskMapper.findAll();
    }

    @Override
    public ProductionTask findById(Long id) {
        return productionTaskMapper.findById(id);
    }

    @Override
    public ProductionTask findByTaskNo(String taskNo) {
        return productionTaskMapper.findByTaskNo(taskNo);
    }

    @Override
    public List<ProductionTask> findByPlanId(Long planId) {
        return productionTaskMapper.findByPlanId(planId);
    }

    /**
     * 新增生产任务；任务必须来自生产计划，指派了操作工则进入已派发，否则等待领取。
     */
    @Override
    @Transactional
    public ProductionTask create(ProductionTask productionTask) {
        if (productionTask.getPlanId() == null) {
            throw new RuntimeException("来源生产计划不能为空");
        }
        foundationService.protectPlan(productionTask.getPlanId());
        ProductionPlan plan = productionPlanMapper.findById(productionTask.getPlanId());
        if (plan == null) {
            throw new RuntimeException("来源生产计划不存在");
        }
        Date now = new Date();
        productionTask.setCreateTime(now);
        productionTask.setUpdateTime(now);
        if (productionTask.getAssignedUserId() == null) {
            productionTask.setTaskStatus(ProductionTask.STATUS_PENDING_CLAIM);
        }
        if (productionTask.getTaskStatus() == null) {
            productionTask.setTaskStatus(productionTask.getAssignedUserId() == null ? ProductionTask.STATUS_PENDING_CLAIM : ProductionTask.STATUS_CLAIMED);
        }
        if (productionTask.getProductId() == null) {
            productionTask.setProductId(plan.getProductId() == null ? 1L : plan.getProductId());
        }
        if (isBlank(productionTask.getProductName())) {
            productionTask.setProductName(plan.getProductName());
        }
        if (productionTask.getQuantity() == null || productionTask.getQuantity() <= 0) {
            productionTask.setQuantity(plan.getPlanQuantity() == null ? 1 : plan.getPlanQuantity());
        }
        List<ProductionTask> existingTasks = productionTaskMapper.findByPlanId(productionTask.getPlanId());
        int existingTotal = 0;
        if (existingTasks != null) {
            for (ProductionTask task : existingTasks) {
                if (task.getQuantity() != null) {
                    existingTotal += task.getQuantity();
                }
            }
        }
        if (existingTotal + productionTask.getQuantity() > (plan.getPlanQuantity() == null ? 0 : plan.getPlanQuantity())) {
            throw new RuntimeException("任务数量超过计划数量，当前计划已分配 " + existingTotal + "，本次新增 " + productionTask.getQuantity() + "，计划总量 " + plan.getPlanQuantity());
        }
        if (productionTask.getTaskType() == null) {
            productionTask.setTaskType(1);
        }
        productionTaskMapper.insert(productionTask);
        markPlanRunningAndOrderProducing(plan);
        return productionTask;
    }

    /**
     * 修改生产任务；修改后重新汇总计划和订单进度。
     */
    @Override
    @Transactional
    public ProductionTask update(ProductionTask productionTask) {
        foundationService.protectTask(productionTask.getId());
        ProductionTask existing = productionTaskMapper.findById(productionTask.getId());
        if (existing == null) {
            throw new RuntimeException("生产任务不存在");
        }
        if (productionTask.getTaskNo() != null) {
            existing.setTaskNo(productionTask.getTaskNo());
        }
        if (productionTask.getPlanId() != null) {
            existing.setPlanId(productionTask.getPlanId());
        }
        if (productionTask.getTaskType() != null) {
            existing.setTaskType(productionTask.getTaskType());
        }
        if (productionTask.getProductId() != null) {
            existing.setProductId(productionTask.getProductId());
        }
        if (productionTask.getProductName() != null) {
            existing.setProductName(productionTask.getProductName());
        }
        if (productionTask.getQuantity() != null) {
            existing.setQuantity(productionTask.getQuantity());
        }
        if (productionTask.getAssignedUserId() != null) {
            existing.setAssignedUserId(productionTask.getAssignedUserId());
        }
        if (productionTask.getAssignedUserName() != null) {
            existing.setAssignedUserName(productionTask.getAssignedUserName());
        }
        if (productionTask.getTaskStatus() != null) {
            existing.setTaskStatus(productionTask.getTaskStatus());
        }
        if (productionTask.getStartTime() != null) {
            existing.setStartTime(productionTask.getStartTime());
        }
        if (productionTask.getEndTime() != null) {
            existing.setEndTime(productionTask.getEndTime());
        }
        if (productionTask.getRemark() != null) {
            existing.setRemark(productionTask.getRemark());
        }
        existing.setUpdateTime(new Date());
        productionTaskMapper.update(existing);
        syncPlanAndOrderAfterTaskProgress(existing.getPlanId());
        return productionTaskMapper.findById(productionTask.getId());
    }

    @Override
    public void delete(Long id) {
        foundationService.protectTask(id);
        productionTaskMapper.delete(id);
    }

    /**
     * 操作工领取待领取任务；使用带状态条件的更新避免重复领取。
     */
    @Override
    public ProductionTask claim(Long taskId, Long userId, String userName) {
        if (userId == null || isBlank(userName)) {
            throw new RuntimeException("领取人信息不能为空");
        }
        ProductionTask task = productionTaskMapper.findById(taskId);
        if (task == null) {
            throw new RuntimeException("生产任务不存在");
        }
        Date now = new Date();
        int updated = productionTaskMapper.claim(taskId, userId, userName.trim(), now);
        if (updated == 0) {
            throw new RuntimeException("任务已被领取或当前状态不允许领取");
        }
        return productionTaskMapper.findById(taskId);
    }

    /**
     * 操作工开始已派发给自己的任务。
     */
    @Override
    public ProductionTask start(Long taskId, Long userId) {
        foundationService.checkTaskPredecessors(taskId);
        if (userId == null) {
            throw new RuntimeException("操作人不能为空");
        }
        ProductionTask task = productionTaskMapper.findById(taskId);
        if (task == null) {
            throw new RuntimeException("生产任务不存在");
        }
        Date now = new Date();
        int updated = productionTaskMapper.start(taskId, userId, now, now);
        if (updated == 0) {
            throw new RuntimeException("只有任务负责人可以开始已派发的任务");
        }
        return productionTaskMapper.findById(taskId);
    }

    /**
     * 保存生产报工；已派发任务首次报工时自动进入生产中，累计完成后进入待质检。
     */
    @Override
    @Transactional
    public ProductionTask report(Long taskId, Long userId, String userName, Integer reportQuantity, String remark) {
        foundationService.lockTask(taskId);
        if (userId == null || isBlank(userName)) {
            throw new RuntimeException("报工人信息不能为空");
        }
        if (reportQuantity == null || reportQuantity <= 0) {
            throw new RuntimeException("本次完成数量必须大于0");
        }
        ProductionTask task = productionTaskMapper.findById(taskId);
        if (task == null) {
            throw new RuntimeException("生产任务不存在");
        }
        Integer taskStatus = task.getTaskStatus();
        if (taskStatus == null || (taskStatus != ProductionTask.STATUS_CLAIMED && taskStatus != ProductionTask.STATUS_IN_PROGRESS)) {
            throw new RuntimeException("只有已派发或生产中的任务可以报工");
        }
        if (!userId.equals(task.getAssignedUserId())) {
            throw new RuntimeException("只有任务负责人可以报工");
        }

        foundationService.checkTaskPredecessors(taskId);
        int completedQuantity = getTaskReportedQuantity(taskId);
        int totalAfterReport = completedQuantity + reportQuantity;
        if (task.getQuantity() == null || totalAfterReport > task.getQuantity()) {
            throw new RuntimeException("累计报工数量不能超过任务数量");
        }

        ProductionReport report = new ProductionReport();
        report.setTaskId(taskId);
        report.setReportUserId(userId);
        report.setReportUserName(userName.trim());
        report.setReportQuantity(reportQuantity);
        report.setQualifiedQuantity(0);
        report.setDefectQuantity(0);
        report.setReportTime(new Date());
        report.setRemark(remark);
        productionReportMapper.insert(report);

        if (taskStatus == ProductionTask.STATUS_CLAIMED) {
            task.setTaskStatus(ProductionTask.STATUS_IN_PROGRESS);
            if (task.getStartTime() == null) {
                task.setStartTime(new Date());
            }
        }
        if (totalAfterReport == task.getQuantity()) {
            task.setTaskStatus(ProductionTask.STATUS_PENDING_QC);
            task.setEndTime(new Date());
        }
        task.setUpdateTime(new Date());
        productionTaskMapper.update(task);
        syncPlanAndOrderAfterTaskProgress(task.getPlanId());
        return productionTaskMapper.findById(taskId);
    }

    /**
     * 任务下发后，生产计划进入进行中，来源订单进入生产中。
     */
    private void markPlanRunningAndOrderProducing(ProductionPlan plan) {
        if (plan == null) {
            return;
        }
        plan.setPlanStatus(PLAN_STATUS_RUNNING);
        plan.setUpdateTime(new Date());
        productionPlanMapper.update(plan);
        if (plan.getOrderId() != null) {
            orderMapper.updateStatus(plan.getOrderId(), ORDER_STATUS_PRODUCING, new Date());
        }
    }

    /**
     * 汇总计划下所有任务进度；全部任务达到待质检后，计划和来源订单进入完成。
     */
    private void syncPlanAndOrderAfterTaskProgress(Long planId) {
        foundationService.syncProgress(planId);
    }

    /**
     * 获取某个任务已累计报工的数量。
     */
    private int getTaskReportedQuantity(Long taskId) {
        ProductionReport summary = productionReportMapper.getTaskSummary(taskId);
        return summary == null ? 0 : safeQuantity(summary.getReportQuantity());
    }

    /** 数量为空时按 0 处理。 */
    private int safeQuantity(Integer quantity) {
        return quantity == null ? 0 : quantity;
    }

    /** 计划数量为空时按 1 处理，避免空计划被误判为完成。 */
    private int safePlanQuantity(Integer quantity) {
        return quantity == null || quantity <= 0 ? 1 : quantity;
    }

    /** 判断字符串是否为空。 */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}