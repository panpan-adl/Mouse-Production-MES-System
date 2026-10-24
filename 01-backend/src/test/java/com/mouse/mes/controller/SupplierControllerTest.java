package com.mouse.mes.controller;

import com.mouse.mes.entity.Supplier;
import com.mouse.mes.util.JsonResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SupplierControllerTest {

    /**
     * 供应商控制器，用于测试供应商接口返回结果。
     */
    @Autowired
    private SupplierController supplierController;

    /**
     * 测试供应商新增接口。
     */
    @Test
    public void testAddSupplier() {
        JsonResult<Supplier> result = supplierController.createSupplier(buildSupplier("CTRL-ADD"));
        System.out.println("供应商新增接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 测试供应商列表查询接口。
     */
    @Test
    public void testGetAllSuppliers() {
        JsonResult<List<Supplier>> result = supplierController.getAllSuppliers();
        System.out.println("供应商列表接口数量：" + result.getData().size());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    /**
     * 测试供应商组合查询接口。
     */
    @Test
    public void testSearchSuppliers() {
        Supplier supplier = supplierController.createSupplier(buildSupplier("CTRL-SEARCH")).getData();
        JsonResult<List<Supplier>> result = supplierController.searchSuppliers("SUP-CTRL-SEARCH", "测试供应商", 1);
        boolean exists = result.getData().stream().anyMatch(item -> item.getId().equals(supplier.getId()));
        System.out.println("供应商组合查询接口数量：" + result.getData().size());
        assertEquals(200, result.getCode());
        assertEquals(true, exists);
    }

    /**
     * 测试供应商修改接口。
     */
    @Test
    public void testUpdateSupplier() {
        Supplier supplier = supplierController.createSupplier(buildSupplier("CTRL-UPDATE")).getData();
        supplier.setContactPerson("控制器修改联系人");
        JsonResult<Supplier> result = supplierController.updateSupplier(supplier.getId(), supplier);
        System.out.println("供应商修改接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals("控制器修改联系人", result.getData().getContactPerson());
    }

    /**
     * 测试供应商禁用接口。
     */
    @Test
    public void testDisableSupplier() {
        Supplier supplier = supplierController.createSupplier(buildSupplier("CTRL-DISABLE")).getData();
        JsonResult<Supplier> result = supplierController.disableSupplier(supplier.getId());
        System.out.println("供应商禁用接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals(0, result.getData().getStatus());
    }

    /**
     * 测试供应商启用接口。
     */
    @Test
    public void testEnableSupplier() {
        Supplier supplier = supplierController.createSupplier(buildSupplier("CTRL-ENABLE")).getData();
        supplierController.disableSupplier(supplier.getId());
        JsonResult<Supplier> result = supplierController.enableSupplier(supplier.getId());
        System.out.println("供应商启用接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getStatus());
    }

    /**
     * 测试供应商删除接口。
     */
    @Test
    public void testDeleteSupplier() {
        Supplier supplier = supplierController.createSupplier(buildSupplier("CTRL-DELETE")).getData();
        JsonResult<Void> result = supplierController.deleteSupplier(supplier.getId());
        System.out.println("供应商删除接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
    }

    /**
     * 构造测试供应商。
     */
    private Supplier buildSupplier(String scene) {
        Supplier supplier = new Supplier();
        supplier.setSupplierCode("SUP-" + scene + "-" + System.currentTimeMillis());
        supplier.setSupplierName("测试供应商-" + scene);
        supplier.setContactPerson("测试联系人");
        supplier.setContactPhone("13800000000");
        supplier.setAddress("测试地址");
        supplier.setStatus(1);
        supplier.setRemark("供应商控制器测试数据");
        supplier.setCreateTime(new Date());
        supplier.setBankName("测试银行");
        supplier.setBankAccount("6222000000000001");
        return supplier;
    }
}
