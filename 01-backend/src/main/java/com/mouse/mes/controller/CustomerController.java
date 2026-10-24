package com.mouse.mes.controller;

import com.mouse.mes.entity.Customer;
import com.mouse.mes.mapper.CustomerMapper;
import com.mouse.mes.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @Autowired
    private CustomerMapper customerMapper;

    @GetMapping("/list")
    public JsonResult<List<Customer>> findAll() {
        return JsonResult.success(customerMapper.findAll());
    }

    @GetMapping("/{id}")
    public JsonResult<Customer> findById(@PathVariable Long id) {
        Customer customer = customerMapper.findById(id);
        if (customer == null) {
            return JsonResult.error("客户不存在");
        }
        return JsonResult.success(customer);
    }

    @GetMapping("/code/{customerCode}")
    public JsonResult<Customer> findByCustomerCode(@PathVariable String customerCode) {
        Customer customer = customerMapper.findByCustomerCode(customerCode);
        if (customer == null) {
            return JsonResult.error("客户不存在");
        }
        return JsonResult.success(customer);
    }

    @PostMapping("/add")
    public JsonResult<Customer> create(@RequestBody Customer customer) {
        if (customer.getCustomerCode() == null || customer.getCustomerCode().isEmpty()) {
            return JsonResult.error("客户编码不能为空");
        }
        if (customer.getCustomerName() == null || customer.getCustomerName().isEmpty()) {
            return JsonResult.error("客户名称不能为空");
        }
        customer.setCreatedAt(new Date());
        customer.setUpdatedAt(new Date());
        customer.setIsDeleted(0);
        if (customer.getStatus() == null) {
            customer.setStatus("ACTIVE");
        }
        customerMapper.insert(customer);
        return JsonResult.success(customer);
    }

    @PutMapping("/{id}")
    public JsonResult<Customer> update(@PathVariable Long id, @RequestBody Customer customer) {
        Customer existing = customerMapper.findById(id);
        if (existing == null) {
            return JsonResult.error("客户不存在");
        }
        customer.setId(id);
        customer.setUpdatedAt(new Date());
        customerMapper.update(customer);
        return JsonResult.success(customerMapper.findById(id));
    }

    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Long id) {
        Customer existing = customerMapper.findById(id);
        if (existing == null) {
            return JsonResult.error("客户不存在");
        }
        existing.setIsDeleted(1);
        existing.setUpdatedAt(new Date());
        customerMapper.update(existing);
        return JsonResult.success();
    }
}
