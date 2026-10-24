package com.mouse.mes.controller;

import com.mouse.mes.entity.Customer;
import com.mouse.mes.util.JsonResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CustomerControllerTest {

    @Autowired
    private CustomerController customerController;

    @Test
    public void testAddCustomer() {
        JsonResult<Customer> result = customerController.create(buildCustomer("CTRL-ADD"));
        System.out.println("客户新增接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    public void testAddCustomerMissingCustomerCode() {
        Customer customer = buildCustomer("CTRL-EMPTY-CODE");
        customer.setCustomerCode("");
        JsonResult<Customer> result = customerController.create(customer);
        System.out.println("客户编码为空接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
    }

    @Test
    public void testAddCustomerMissingCustomerName() {
        Customer customer = buildCustomer("CTRL-EMPTY-NAME");
        customer.setCustomerName("");
        JsonResult<Customer> result = customerController.create(customer);
        System.out.println("客户名称为空接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
    }

    @Test
    public void testGetAllCustomers() {
        JsonResult<List<Customer>> result = customerController.findAll();
        System.out.println("客户列表接口数量：" + result.getData().size());
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    public void testGetCustomerById() {
        Customer saved = customerController.create(buildCustomer("CTRL-FIND")).getData();
        JsonResult<Customer> result = customerController.findById(saved.getId());
        System.out.println("客户查询接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals(saved.getCustomerCode(), result.getData().getCustomerCode());
    }

    @Test
    public void testGetCustomerByIdNotFound() {
        JsonResult<Customer> result = customerController.findById(-999999L);
        System.out.println("客户不存在接口结果：" + result.getMsg());
        assertEquals(500, result.getCode());
    }

    @Test
    public void testGetCustomerByCustomerCode() {
        Customer saved = customerController.create(buildCustomer("CTRL-FIND-CODE")).getData();
        JsonResult<Customer> result = customerController.findByCustomerCode(saved.getCustomerCode());
        System.out.println("按编码查询客户接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals(saved.getId(), result.getData().getId());
    }

    @Test
    public void testUpdateCustomer() {
        Customer saved = customerController.create(buildCustomer("CTRL-UPDATE")).getData();
        saved.setContactPerson("控制器修改联系人");
        JsonResult<Customer> result = customerController.update(saved.getId(), saved);
        System.out.println("客户修改接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals("控制器修改联系人", result.getData().getContactPerson());
    }

    @Test
    public void testDeleteCustomer() {
        Customer saved = customerController.create(buildCustomer("CTRL-DELETE")).getData();
        JsonResult<Void> result = customerController.delete(saved.getId());
        JsonResult<Customer> findResult = customerController.findById(saved.getId());
        System.out.println("客户删除接口结果：" + result.getMsg());
        assertEquals(200, result.getCode());
        assertEquals(500, findResult.getCode());
    }

    private Customer buildCustomer(String scene) {
        Customer customer = new Customer();
        customer.setCustomerCode("CUS-" + scene + "-" + System.nanoTime());
        customer.setCustomerName("测试客户-" + scene);
        customer.setContactPerson("测试联系人");
        customer.setPhone("13800000000");
        customer.setAddress("测试地址");
        customer.setStatus("ACTIVE");
        return customer;
    }
}