package com.mouse.mes.service;

import com.mouse.mes.constant.QcInspectionResult;
import com.mouse.mes.constant.QcInspectionType;
import com.mouse.mes.entity.QcInspection;
import com.mouse.mes.entity.QcInspectionItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class QcInspectionServiceTest {

    /**
     * 质检业务服务，用于测试质检单新增、修改、删除、查询、合格率和不合格处理。
     */
    @Autowired
    private QcInspectionService qcInspectionService;

    /**
     * 测试新增质检单，新增后数据库能查到。
     */
    @Test
    public void testAddQcInspection() {
        QcInspection inspection = buildInspection("ADD", QcInspectionType.INCOMING, QcInspectionResult.QUALIFIED);
        QcInspection savedInspection = qcInspectionService.create(inspection);
        QcInspection dbInspection = qcInspectionService.findById(savedInspection.getId());
        System.out.println("新增质检单结果：" + dbInspection);
        assertNotNull(dbInspection);
        assertEquals(savedInspection.getInspectionNo(), dbInspection.getInspectionNo());
        assertEquals(2, dbInspection.getItems().size());
    }

    /**
     * 测试质检单号为空时失败。
     */
    @Test
    public void testInspectionNoEmptyShouldFail() {
        QcInspection inspection = buildInspection("EMPTY-NO", QcInspectionType.INCOMING, QcInspectionResult.QUALIFIED);
        inspection.setInspectionNo("");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> qcInspectionService.create(inspection));
        System.out.println("质检单号为空结果：" + exception.getMessage());
        assertEquals("质检单号不能为空", exception.getMessage());
    }

    /**
     * 测试修改质检结果后数据库字段更新。
     */
    @Test
    public void testUpdateQcInspectionResult() {
        QcInspection inspection = qcInspectionService.create(buildInspection("UPDATE", QcInspectionType.PRODUCTION, QcInspectionResult.QUALIFIED));
        inspection.setResult(QcInspectionResult.REWORK);
        inspection.setDefectReason("按键压力不稳定");
        inspection.setHandleMethod("退回生产线返工");
        QcInspection updatedInspection = qcInspectionService.update(inspection);
        QcInspection dbInspection = qcInspectionService.findById(updatedInspection.getId());
        System.out.println("修改质检结果后查询：" + dbInspection);
        assertEquals(QcInspectionResult.REWORK, dbInspection.getResult());
        assertEquals("NEED_REWORK", dbInspection.getBusinessStatus());
    }

    /**
     * 测试删除质检单后按ID查询不到。
     */
    @Test
    public void testDeleteQcInspection() {
        QcInspection inspection = qcInspectionService.create(buildInspection("DELETE", QcInspectionType.FINISHED, QcInspectionResult.QUALIFIED));
        qcInspectionService.delete(inspection.getId());
        QcInspection dbInspection = qcInspectionService.findById(inspection.getId());
        System.out.println("删除质检单后查询结果：" + dbInspection);
        assertNull(dbInspection);
    }

    /**
     * 测试删除不存在质检单时抛出“不存在”。
     */
    @Test
    public void testDeleteNotExistsShouldFail() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> qcInspectionService.delete(-999999L));
        System.out.println("删除不存在质检单结果：" + exception.getMessage());
        assertEquals("质检单不存在", exception.getMessage());
    }

    /**
     * 测试合格数量不能大于检验数量。
     */
    @Test
    public void testQualifiedQuantityCannotGreaterThanSampleQuantity() {
        QcInspection inspection = buildInspection("QTY-FAIL", QcInspectionType.INCOMING, QcInspectionResult.QUALIFIED);
        inspection.setItems(null);
        inspection.setSampleQuantity(new BigDecimal("10"));
        inspection.setQualifiedQuantity(new BigDecimal("11"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> qcInspectionService.create(inspection));
        System.out.println("合格数量大于检验数量结果：" + exception.getMessage());
        assertEquals("合格数量不能大于检验数量", exception.getMessage());
    }

    /**
     * 测试合格率计算正确。
     */
    @Test
    public void testQualifiedRateCalculateCorrectly() {
        QcInspection inspection = buildInspection("RATE", QcInspectionType.INCOMING, QcInspectionResult.QUALIFIED);
        inspection.setItems(null);
        inspection.setSampleQuantity(new BigDecimal("20"));
        inspection.setQualifiedQuantity(new BigDecimal("19"));
        inspection.setResult(QcInspectionResult.CONCESSION_ACCEPT);
        QcInspection savedInspection = qcInspectionService.create(inspection);
        System.out.println("合格率计算结果：" + savedInspection.getQualifiedRate());
        assertEquals(0, savedInspection.getQualifiedRate().compareTo(new BigDecimal("95.00")));
        assertEquals(0, savedInspection.getDefectiveQuantity().compareTo(new BigDecimal("1")));
    }

    /**
     * 测试不合格记录能保存原因和处理方式。
     */
    @Test
    public void testUnqualifiedReasonAndHandleMethodSaved() {
        QcInspection inspection = buildInspection("UNQUALIFIED", QcInspectionType.INCOMING, QcInspectionResult.UNQUALIFIED);
        inspection.setItems(Arrays.asList(
                buildItem("外观检查", new BigDecimal("10"), new BigDecimal("8")),
                buildItem("尺寸检查", new BigDecimal("10"), new BigDecimal("10"))
        ));
        inspection.setDefectReason("外壳划痕超标");
        inspection.setHandleMethod("退回供应商");
        QcInspection savedInspection = qcInspectionService.create(inspection);
        QcInspection dbInspection = qcInspectionService.findById(savedInspection.getId());
        System.out.println("不合格记录结果：" + dbInspection);
        assertEquals("外壳划痕超标", dbInspection.getDefectReason());
        assertEquals("退回供应商", dbInspection.getHandleMethod());
        assertEquals(0, dbInspection.getDefectiveQuantity().compareTo(new BigDecimal("2.00")));
        assertEquals("BLOCKED", dbInspection.getBusinessStatus());
    }

    /**
     * 测试按质检类型查询。
     */
    @Test
    public void testFindByInspectionType() {
        QcInspection inspection = qcInspectionService.create(buildInspection("TYPE", QcInspectionType.PRODUCTION, QcInspectionResult.QUALIFIED));
        List<QcInspection> inspectionList = qcInspectionService.findByInspectionType(QcInspectionType.PRODUCTION);
        boolean exists = inspectionList.stream().anyMatch(item -> item.getId().equals(inspection.getId()));
        System.out.println("按质检类型查询数量：" + inspectionList.size());
        assertTrue(exists);
    }

    /**
     * 构造测试质检单。
     */
    private QcInspection buildInspection(String scene, String inspectionType, String result) {
        QcInspection inspection = new QcInspection();
        inspection.setInspectionNo("QC-" + scene + "-" + System.nanoTime());
        inspection.setInspectionType(inspectionType);
        inspection.setSourceOrderId(1L);
        inspection.setSourceOrderNo("SOURCE-" + System.nanoTime());
        inspection.setResult(result);
        inspection.setRemark("质检测试单据");
        inspection.setCreatedBy(1L);
        inspection.setItems(Arrays.asList(
                buildItem("外观检查", new BigDecimal("10"), new BigDecimal("10")),
                buildItem("尺寸检查", new BigDecimal("10"), new BigDecimal("10"))
        ));
        return inspection;
    }

    /**
     * 构造测试质检明细。
     */
    private QcInspectionItem buildItem(String itemName, BigDecimal sampleQuantity, BigDecimal qualifiedQuantity) {
        QcInspectionItem item = new QcInspectionItem();
        item.setMaterialId(1L);
        item.setInspectionItem(itemName);
        item.setInspectionStandard("符合工艺标准");
        item.setActualValue("实测合格");
        item.setSampleQuantity(sampleQuantity);
        item.setQualifiedQuantity(qualifiedQuantity);
        item.setItemResult(qualifiedQuantity.compareTo(sampleQuantity) == 0
                ? QcInspectionResult.QUALIFIED : QcInspectionResult.UNQUALIFIED);
        item.setRemark("质检明细测试数据");
        return item;
    }
}
