package com.mouse.mes.controller;

import com.mouse.mes.entity.Bom;
import com.mouse.mes.entity.Product;
import com.mouse.mes.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BomControllerTest {

    @Autowired
    private BomController bomController;

    @Autowired
    private ProductService productService;

    @Test
    public void testAddBom() {
        Long productId = getOrCreateProductId("CTRL-BOM-PROD");
        Bom result = bomController.createBom(buildBom(productId, "CTRL-ADD"));
        System.out.println("BOM新增接口结果：ID=" + result.getId());
        assertNotNull(result);
        assertNotNull(result.getId());
    }

    @Test
    public void testGetAllBoms() {
        List<Bom> result = bomController.getAllBoms();
        System.out.println("BOM列表接口数量：" + result.size());
        assertNotNull(result);
    }

    @Test
    public void testGetBomById() {
        Long productId = getOrCreateProductId("CTRL-BOM-FIND-PROD");
        Bom saved = bomController.createBom(buildBom(productId, "CTRL-FIND"));
        Bom result = bomController.getBomById(saved.getId());
        System.out.println("BOM查询接口结果：ID=" + result.getId());
        assertNotNull(result);
        assertEquals(saved.getMaterialName(), result.getMaterialName());
    }

    @Test
    public void testGetBomsByProductId() {
        Long productId = getOrCreateProductId("CTRL-BOM-PROD-QUERY");
        bomController.createBom(buildBom(productId, "CTRL-PROD-QUERY"));
        List<Bom> result = bomController.getBomsByProductId(productId);
        System.out.println("按产品查询BOM接口数量：" + result.size());
        assertNotNull(result);
    }

    @Test
    public void testUpdateBom() {
        Long productId = getOrCreateProductId("CTRL-BOM-UPDATE-PROD");
        Bom saved = bomController.createBom(buildBom(productId, "CTRL-UPDATE"));
        saved.setRemark("控制器修改BOM");
        Bom result = bomController.updateBom(saved.getId(), saved);
        System.out.println("BOM修改接口结果：" + result.getRemark());
        assertNotNull(result);
        assertEquals("控制器修改BOM", result.getRemark());
    }

    @Test
    public void testDeleteBom() {
        Long productId = getOrCreateProductId("CTRL-BOM-DELETE-PROD");
        Bom saved = bomController.createBom(buildBom(productId, "CTRL-DELETE"));
        bomController.deleteBom(saved.getId());
        Bom result = bomController.getBomById(saved.getId());
        System.out.println("BOM删除接口结果：" + (result == null ? "已删除" : "未删除"));
    }

    private Long getOrCreateProductId(String scene) {
        List<Product> products = productService.findAll();
        if (!products.isEmpty()) {
            return products.get(0).getId();
        }
        Product product = productService.create(buildProduct(scene));
        return product.getId() != null ? product.getId() : 1L;
    }

    private Product buildProduct(String scene) {
        Product product = new Product();
        product.setProductCode("PRD-" + scene + "-" + System.nanoTime());
        product.setProductName("测试产品-" + scene);
        product.setSpecification("标准规格");
        product.setUnit("PCS");
        product.setStatus(1);
        product.setCreateTime(new Date());
        product.setUpdateTime(new Date());
        return product;
    }

    private Bom buildBom(Long productId, String scene) {
        Bom bom = new Bom();
        bom.setProductId(productId);
        bom.setMaterialId(1L);
        bom.setMaterialName("测试物料-" + scene);
        bom.setQuantity(new BigDecimal("1.0"));
        bom.setUnit("PCS");
        bom.setRemark("BOM控制器测试数据");
        return bom;
    }
}