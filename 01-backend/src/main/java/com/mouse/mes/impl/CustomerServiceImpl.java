package com.mouse.mes.impl;

import com.mouse.mes.entity.Customer;
import com.mouse.mes.mapper.CustomerMapper;
import com.mouse.mes.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerMapper customerMapper;

    @Override
    public List<Customer> findAll() {
        return customerMapper.findAll();
    }

    @Override
    public Customer findById(Long id) {
        return customerMapper.findById(id);
    }

    @Override
    public Customer findByCode(String customerCode) {
        return customerMapper.findByCustomerCode(customerCode);
    }

    @Override
    public Customer create(Customer customer) {
        customer.setCreatedAt(new Date());
        customer.setUpdatedAt(new Date());
        customer.setIsDeleted(0);
        customerMapper.insert(customer);
        return customer;
    }

    @Override
    public Customer update(Customer customer) {
        customer.setUpdatedAt(new Date());
        customerMapper.update(customer);
        return customerMapper.findById(customer.getId());
    }

    @Override
    public void delete(Long id) {
        customerMapper.delete(id);
    }
}