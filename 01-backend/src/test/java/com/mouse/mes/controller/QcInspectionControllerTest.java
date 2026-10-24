package com.mouse.mes.controller;

import com.mouse.mes.constant.QcInspectionResult;
import com.mouse.mes.constant.QcInspectionType;
import com.mouse.mes.entity.QcInspection;
import com.mouse.mes.entity.QcInspectionItem;
import com.mouse.mes.util.JsonResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class QcInspectionControllerTest {

    /**
     * 质检控制器，用于测试质检接口返回结果。
     */
    @Autowired
    private QcInspectionController qcInspectionController;

    /**
     * 测试新增质检单接口。
     */
    @Test
    public void testAddQcInspection() {
        JsonResult<QcInspection> result = qcInspectionController.create(buildInspection("CTRL-ADD", QcInspectionType.INCOMING, QcInspectionResult.QUALIFIED));
        System.out.println("新增质检单接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 测试质检单号为空时接口返回失败。
     */
    @Test
    public void testInspectionNoEmptyShouldFail() {
        QcInspection inspection = buildInspection("CTRL-EMPTY", QcInspectionType.INCOMING, QcInspectionResult.QUALIFIED);
        inspection.setInspectionNo("");
        JsonResult<QcInspection> result = qcInspectionController.create(inspection);
        System.out.println("质检单号为空接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
    }

    /**
     * 测试修改质检结果接口。
     */
    @Test
    public void testUpdateQcInspectionResult() {
        QcInspection inspection = qcInspectionController.create(buildInspection("CTRL-UPDATE", QcInspectionType.PRODUCTION, QcInspectionResult.QUALIFIED)).getData();
        inspection.setResult(QcInspectionResult.SCRAP);
        inspection.setDefectReason("主板损坏");
        inspection.setHandleMethod("报废并登记");
        JsonResult<QcInspection> result = qcInspectionController.update(inspection.getId(), inspection);
        System.out.println("修改质检结果接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals(QcInspectionResult.SCRAP, result.getData().getResult());
        assertEquals("SCRAPPED", result.getData().getBusinessStatus());
    }

    /**
     * 测试删除不存在质检单时返回“不存在”。
     */
    @Test
    public void testDeleteNotExistsShouldReturnError() {
        JsonResult<Void> result = qcInspectionController.delete(-999999L);
        System.out.println("删除不存在质检单接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
        assertEquals("质检单不存在", result.getMsg());
    }

    /**
     * 测试查询所有质检单接口。
     */
    @Test
    public void testFindAllQcInspection() {
        JsonResult<List<QcInspection>> result = qcInspectionController.findAll();
        System.out.println("质检单列表接口数量：" + result.getData().size());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
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
        inspection.setRemark("质检控制器测试单据");
        inspection.setCreatedBy(1L);
        inspection.setItems(Collections.singletonList(buildItem("外观检查", new BigDecimal("10"), new BigDecimal("10"))));
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
        item.setItemResult(QcInspectionResult.QUALIFIED);
        item.setRemark("质检控制器明细测试数据");
        return item;
    }
}
