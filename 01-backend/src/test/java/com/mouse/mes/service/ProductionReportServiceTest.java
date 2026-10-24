package com.mouse.mes.service;

import com.mouse.mes.entity.ProductionReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ProductionReportServiceTest {

    @Autowired
    private ProductionReportService productionReportService;

    @Test
    public void testFindAll() {
        List<ProductionReport> reports = productionReportService.findAll();
        assertNotNull(reports);
    }

    @Test
    public void testFindById() {
        List<ProductionReport> reports = productionReportService.findAll();
        if (!reports.isEmpty()) {
            Long id = reports.get(0).getId();
            ProductionReport report = productionReportService.findById(id);
            assertNotNull(report);
            assertEquals(id, report.getId());
        }
    }

    @Test
    public void testFindByTaskId() {
        List<ProductionReport> reports = productionReportService.findByTaskId(1L);
        assertNotNull(reports);
    }

    @Test
    public void testCreateProductionReport() {
        ProductionReport report = new ProductionReport();
        report.setTaskId(1L);
        report.setReportUserId(1L);
        report.setReportUserName("服务测试用户");
        report.setReportQuantity(10);
        report.setQualifiedQuantity(8);
        report.setDefectQuantity(2);

        ProductionReport created = productionReportService.create(report);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(1L, created.getTaskId());
        assertEquals(10, created.getReportQuantity());
        assertEquals(8, created.getQualifiedQuantity());
        assertEquals(2, created.getDefectQuantity());
        assertNotNull(created.getReportTime());
    }

    @Test
    public void testCreateReportInvalidQuantity() {
        ProductionReport report = new ProductionReport();
        report.setTaskId(1L);
        report.setReportUserId(1L);
        report.setReportQuantity(10);
        report.setQualifiedQuantity(9);
        report.setDefectQuantity(2);

        assertThrows(RuntimeException.class, () -> productionReportService.create(report));
    }

    @Test
    public void testCreateReportZeroQuantity() {
        ProductionReport report = new ProductionReport();
        report.setTaskId(1L);
        report.setReportUserId(1L);
        report.setReportQuantity(0);
        report.setQualifiedQuantity(0);
        report.setDefectQuantity(0);

        assertThrows(RuntimeException.class, () -> productionReportService.create(report));
    }

    @Test
    public void testUpdateProductionReport() {
        ProductionReport report = new ProductionReport();
        report.setTaskId(1L);
        report.setReportUserId(1L);
        report.setReportUserName("张三");
        report.setReportQuantity(10);
        report.setQualifiedQuantity(8);
        report.setDefectQuantity(2);

        ProductionReport created = productionReportService.create(report);
        created.setReportQuantity(15);
        created.setQualifiedQuantity(12);
        created.setDefectQuantity(3);

        ProductionReport updated = productionReportService.update(created.getId(), created);
        assertNotNull(updated);
        assertEquals(15, updated.getReportQuantity());
        assertEquals(12, updated.getQualifiedQuantity());
        assertEquals(3, updated.getDefectQuantity());
    }

    @Test
    public void testUpdateReportNotFound() {
        ProductionReport report = new ProductionReport();
        report.setReportQuantity(10);
        report.setQualifiedQuantity(10);
        report.setDefectQuantity(0);

        assertThrows(RuntimeException.class, () -> productionReportService.update(999999L, report));
    }

    @Test
    public void testFindByReportUserId() {
        List<ProductionReport> reports = productionReportService.findByReportUserId(1L);
        assertNotNull(reports);
    }

    @Test
    public void testGetTaskSummary() {
        ProductionReport summary = productionReportService.getTaskSummary(1L);
        assertNotNull(summary);
    }

    @Test
    public void testGetTaskSummaryEmpty() {
        ProductionReport summary = productionReportService.getTaskSummary(999999L);
        assertNull(summary);
    }
}