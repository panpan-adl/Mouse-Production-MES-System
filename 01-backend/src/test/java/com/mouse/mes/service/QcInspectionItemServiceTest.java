package com.mouse.mes.service;

import com.mouse.mes.constant.QcInspectionResult;
import com.mouse.mes.constant.QcInspectionType;
import com.mouse.mes.entity.QcInspection;
import com.mouse.mes.entity.QcInspectionItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class QcInspectionItemServiceTest {

    /**
     * 质检单业务服务，用于准备质检主单。
     */
    @Autowired
    private QcInspectionService qcInspectionService;

    /**
     * 质检明细业务服务，用于测试检测项明细查询和修改。
     */
    @Autowired
    private QcInspectionItemService qcInspectionItemService;

    /**
     * 测试质检单明细能按质检单ID查询出来。
     */
    @Test
    public void testFindItemsByInspectionId() {
        QcInspection inspection = qcInspectionService.create(buildInspection("ITEM-FIND"));
        List<QcInspectionItem> itemList = qcInspectionItemService.findByInspectionId(inspection.getId());
        System.out.println("质检明细数量：" + itemList.size());
        assertEquals(1, itemList.size());
        assertEquals("外观检查", itemList.get(0).getInspectionItem());
    }

    /**
     * 测试新增质检明细。
     */
    @Test
    public void testAddQcInspectionItem() {
        QcInspection inspection = qcInspectionService.create(buildInspection("ITEM-ADD"));
        QcInspectionItem item = buildItem("按键寿命", new BigDecimal("5"), new BigDecimal("5"));
        item.setInspectionId(inspection.getId());
        QcInspectionItem savedItem = qcInspectionItemService.create(item);
        System.out.println("新增质检明细结果：" + savedItem);
        assertNotNull(savedItem);
        assertEquals("按键寿命", savedItem.getInspectionItem());
    }

    /**
     * 测试修改质检明细，不合格数量会重新计算。
     */
    @Test
    public void testUpdateQcInspectionItem() {
        QcInspection inspection = qcInspectionService.create(buildInspection("ITEM-UPDATE"));
        QcInspectionItem item = qcInspectionItemService.findByInspectionId(inspection.getId()).get(0);
        item.setQualifiedQuantity(new BigDecimal("8"));
        item.setItemResult(QcInspectionResult.UNQUALIFIED);
        item.setDefectReason("表面划痕");
        item.setHandleMethod("返修抛光");
        QcInspectionItem updatedItem = qcInspectionItemService.update(item);
        System.out.println("修改质检明细结果：" + updatedItem);
        assertEquals(0, updatedItem.getDefectiveQuantity().compareTo(new BigDecimal("2.00")));
        assertEquals("表面划痕", updatedItem.getDefectReason());
    }

    /**
     * 测试按质检单ID删除明细。
     */
    @Test
    public void testDeleteItemsByInspectionId() {
        QcInspection inspection = qcInspectionService.create(buildInspection("ITEM-DELETE"));
        qcInspectionItemService.deleteByInspectionId(inspection.getId());
        List<QcInspectionItem> itemList = qcInspectionItemService.findByInspectionId(inspection.getId());
        System.out.println("删除后质检明细数量：" + itemList.size());
        assertTrue(itemList.isEmpty());
    }

    /**
     * 构造测试质检单。
     */
    private QcInspection buildInspection(String scene) {
        QcInspection inspection = new QcInspection();
        inspection.setInspectionNo("QC-" + scene + "-" + System.nanoTime());
        inspection.setInspectionType(QcInspectionType.INCOMING);
        inspection.setSourceOrderNo("SOURCE-" + System.nanoTime());
        inspection.setResult(QcInspectionResult.QUALIFIED);
        inspection.setCreatedBy(1L);
        inspection.setRemark("质检明细测试单据");
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
        item.setItemResult(qualifiedQuantity.compareTo(sampleQuantity) == 0
                ? QcInspectionResult.QUALIFIED : QcInspectionResult.UNQUALIFIED);
        item.setRemark("质检明细测试数据");
        return item;
    }
}
