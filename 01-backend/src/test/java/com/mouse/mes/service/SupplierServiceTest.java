package com.mouse.mes.service;

import com.mouse.mes.entity.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class SupplierServiceTest {

    /**
     * 供应商业务服务，用于测试供应商新增、修改、删除、查询和禁用。
     */
    @Autowired
    private SupplierService supplierService;

    /**
     * 测试新增供应商，新增后再查询数据库中是否存在。
     */
    @Test
    public void testAddSupplier() {
        Supplier supplier = buildSupplier("ADD");
        Supplier savedSupplier = supplierService.create(supplier);
        Supplier dbSupplier = supplierService.findById(savedSupplier.getId());
        System.out.println("新增供应商结果：" + dbSupplier);
        assertNotNull(dbSupplier);
        assertEquals(savedSupplier.getSupplierCode(), dbSupplier.getSupplierCode());
    }

    /**
     * 测试查询全部供应商。
     */
    @Test
    public void testGetAllSuppliers() {
        List<Supplier> supplierList = supplierService.findAll();
        System.out.println("供应商数量：" + supplierList.size());
        for (Supplier supplier : supplierList) {
            System.out.println(supplier.getSupplierCode() + " - " + supplier.getSupplierName());
        }
        assertNotNull(supplierList);
    }

    /**
     * 测试按编码、名称和状态组合查询供应商。
     */
    @Test
    public void testSearchSuppliers() {
        Supplier supplier = supplierService.create(buildSupplier("SEARCH"));
        List<Supplier> supplierList = supplierService.search("SUP-SEARCH", "测试供应商", 1);
        boolean exists = supplierList.stream().anyMatch(item -> item.getId().equals(supplier.getId()));
        System.out.println("组合查询供应商数量：" + supplierList.size());
        assertEquals(true, exists);
    }

    /**
     * 测试按供应商编码查询供应商。
     */
    @Test
    public void testGetSupplierByCode() {
        Supplier supplier = supplierService.create(buildSupplier("CODE"));
        Supplier dbSupplier = supplierService.findBySupplierCode(supplier.getSupplierCode());
        System.out.println("按编码查询供应商结果：" + dbSupplier);
        assertNotNull(dbSupplier);
        assertEquals(supplier.getSupplierCode(), dbSupplier.getSupplierCode());
    }

    /**
     * 测试修改供应商，修改后查询数据库字段是否变化。
     */
    @Test
    public void testUpdateSupplier() {
        Supplier supplier = supplierService.create(buildSupplier("UPDATE"));
        supplier.setContactPhone("13900000000");
        supplier.setRemark("供应商修改测试");
        Supplier updatedSupplier = supplierService.update(supplier);
        System.out.println("修改供应商结果：" + updatedSupplier);
        assertNotNull(updatedSupplier);
        assertEquals("13900000000", updatedSupplier.getContactPhone());
        assertEquals("供应商修改测试", updatedSupplier.getRemark());
    }

    /**
     * 测试删除供应商，删除后按ID查询应查询不到。
     */
    @Test
    public void testDeleteSupplier() {
        Supplier supplier = supplierService.create(buildSupplier("DELETE"));
        supplierService.delete(supplier.getId());
        Supplier dbSupplier = supplierService.findById(supplier.getId());
        System.out.println("删除供应商后查询结果：" + dbSupplier);
        assertNull(dbSupplier);
    }

    /**
     * 测试禁用供应商，禁用后仍可查询到，但状态应变为0。
     */
    @Test
    public void testDisableSupplier() {
        Supplier supplier = supplierService.create(buildSupplier("DISABLE"));
        Supplier disabledSupplier = supplierService.disable(supplier.getId());
        Supplier dbSupplier = supplierService.findById(supplier.getId());
        System.out.println("禁用供应商结果：" + disabledSupplier);
        assertEquals(0, disabledSupplier.getStatus());
        assertNotNull(dbSupplier);
        assertEquals(0, dbSupplier.getStatus());
    }

    /**
     * 测试通过update恢复禁用供应商。
     */
    @Test
    public void testUpdateCanEnableDisabledSupplier() {
        Supplier supplier = supplierService.create(buildSupplier("ENABLE-BY-UPDATE"));
        supplierService.disable(supplier.getId());
        Supplier disabledSupplier = supplierService.findById(supplier.getId());
        disabledSupplier.setStatus(1);
        Supplier enabledSupplier = supplierService.update(disabledSupplier);
        System.out.println("通过修改恢复供应商结果：" + enabledSupplier);
        assertEquals(1, enabledSupplier.getStatus());
    }

    /**
     * 测试启用供应商快捷方法。
     */
    @Test
    public void testEnableSupplier() {
        Supplier supplier = supplierService.create(buildSupplier("ENABLE"));
        supplierService.disable(supplier.getId());
        Supplier enabledSupplier = supplierService.enable(supplier.getId());
        System.out.println("启用供应商结果：" + enabledSupplier);
        assertEquals(1, enabledSupplier.getStatus());
    }

    /**
     * 构造测试供应商。
     */
    private Supplier buildSupplier(String scene) {
        String uniqueCode = "SUP-" + scene + "-" + System.currentTimeMillis();
        Supplier supplier = new Supplier();
        supplier.setSupplierCode(uniqueCode);
        supplier.setSupplierName("测试供应商-" + scene);
        supplier.setContactPerson("测试联系人");
        supplier.setContactPhone("13800000000");
        supplier.setAddress("测试地址");
        supplier.setBankName("测试银行");
        supplier.setBankAccount("6222000000000000");
        supplier.setStatus(1);
        supplier.setRemark("供应商测试数据");
        return supplier;
    }
}
