package com.mouse.mes.controller;

import com.mouse.mes.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProductControllerTest {

    @Autowired
    private ProductController productController;

    @Test
    public void testAddProduct() {
        Product result = productController.createProduct(buildProduct("CTRL-ADD"));
        System.out.println("产品新增接口结果：" + result.getProductCode());
        assertNotNull(result);
        assertNotNull(result.getId());
    }

    @Test
    public void testGetAllProducts() {
        List<Product> result = productController.getAllProducts();
        System.out.println("产品列表接口数量：" + result.size());
        assertNotNull(result);
    }

    @Test
    public void testGetProductById() {
        Product saved = productController.createProduct(buildProduct("CTRL-FIND"));
        Product result = productController.getProductById(saved.getId());
        System.out.println("产品查询接口结果：" + result.getProductCode());
        assertNotNull(result);
        assertEquals(saved.getProductCode(), result.getProductCode());
    }

    @Test
    public void testGetProductByProductCode() {
        Product saved = productController.createProduct(buildProduct("CTRL-FIND-CODE"));
        Product result = productController.getProductByProductCode(saved.getProductCode());
        System.out.println("按编码查询产品接口结果：" + result.getProductName());
        assertNotNull(result);
        assertEquals(saved.getId(), result.getId());
    }

    @Test
    public void testUpdateProduct() {
        Product saved = productController.createProduct(buildProduct("CTRL-UPDATE"));
        saved.setProductName("控制器修改产品");
        Product result = productController.updateProduct(saved.getId(), saved);
        System.out.println("产品修改接口结果：" + result.getProductName());
        assertNotNull(result);
        assertEquals("控制器修改产品", result.getProductName());
    }

    @Test
    public void testDeleteProduct() {
        Product saved = productController.createProduct(buildProduct("CTRL-DELETE"));
        productController.deleteProduct(saved.getId());
        Product result = productController.getProductById(saved.getId());
        System.out.println("产品删除接口结果：" + (result == null ? "已删除" : "未删除"));
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
}