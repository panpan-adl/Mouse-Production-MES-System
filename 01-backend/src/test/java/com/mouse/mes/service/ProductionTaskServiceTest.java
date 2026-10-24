package com.mouse.mes.service;

import com.mouse.mes.entity.ProductionTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ProductionTaskServiceTest {

    @Autowired
    private ProductionTaskService productionTaskService;

    @Test
    public void testFindAll() {
        List<ProductionTask> tasks = productionTaskService.findAll();
        assertNotNull(tasks);
    }

    @Test
    public void testFindById() {
        List<ProductionTask> tasks = productionTaskService.findAll();
        if (!tasks.isEmpty()) {
            Long id = tasks.get(0).getId();
            ProductionTask task = productionTaskService.findById(id);
            assertNotNull(task);
            assertEquals(id, task.getId());
        }
    }

    @Test
    public void testFindByTaskNo() {
        List<ProductionTask> tasks = productionTaskService.findAll();
        if (!tasks.isEmpty()) {
            String taskNo = tasks.get(0).getTaskNo();
            ProductionTask task = productionTaskService.findByTaskNo(taskNo);
            assertNotNull(task);
            assertEquals(taskNo, task.getTaskNo());
        }
    }

    @Test
    public void testFindByPlanId() {
        List<ProductionTask> tasks = productionTaskService.findByPlanId(1L);
        assertNotNull(tasks);
    }

    @Test
    public void testCreateProductionTask() {
        ProductionTask task = new ProductionTask();
        task.setTaskNo("TASK_SERVICE_TEST" + System.currentTimeMillis());
        task.setPlanId(1L);
        task.setTaskType(1);
        task.setProductId(1L);
        task.setProductName("测试产品");
        task.setQuantity(100);
        task.setTaskStatus(0);

        ProductionTask created = productionTaskService.create(task);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(task.getTaskNo(), created.getTaskNo());
        assertNotNull(created.getCreateTime());
        assertNotNull(created.getUpdateTime());
    }

    @Test
    public void testUpdateProductionTask() {
        ProductionTask task = new ProductionTask();
        task.setTaskNo("TASK_UPDATE_TEST" + System.currentTimeMillis());
        task.setPlanId(1L);
        task.setTaskType(1);
        task.setProductId(1L);
        task.setProductName("测试产品");
        task.setQuantity(100);
        task.setTaskStatus(0);

        ProductionTask created = productionTaskService.create(task);
        created.setTaskStatus(1);

        ProductionTask updated = productionTaskService.update(created);
        assertNotNull(updated);
        assertEquals(1, updated.getTaskStatus());
    }

    @Test
    public void testDeleteProductionTask() {
        ProductionTask task = new ProductionTask();
        task.setTaskNo("TASK_DELETE_TEST" + System.currentTimeMillis());
        task.setPlanId(1L);
        task.setTaskType(1);
        task.setProductId(1L);
        task.setProductName("测试产品");
        task.setQuantity(100);
        task.setTaskStatus(0);

        ProductionTask created = productionTaskService.create(task);
        productionTaskService.delete(created.getId());

        assertNull(productionTaskService.findById(created.getId()));
    }
}