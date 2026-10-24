package com.mouse.mes.service;

import com.mouse.mes.entity.ProductionTask;

import java.util.List;

public interface ProductionTaskService {
    List<ProductionTask> findAll();
    ProductionTask findById(Long id);
    ProductionTask findByTaskNo(String taskNo);
    List<ProductionTask> findByPlanId(Long planId);
    ProductionTask create(ProductionTask productionTask);
    ProductionTask update(ProductionTask productionTask);
    void delete(Long id);

    /**
     * 将待领取任务原子地分配给一名操作工。
     */
    ProductionTask claim(Long taskId, Long userId, String userName);

    /**
     * 由已领取该任务的操作工开始生产。
     */
    ProductionTask start(Long taskId, Long userId);

    /**
     * 记录操作工本次完成数量；全部数量报工后，任务进入待质检状态。
     */
    ProductionTask report(Long taskId, Long userId, String userName, Integer reportQuantity, String remark);
}
