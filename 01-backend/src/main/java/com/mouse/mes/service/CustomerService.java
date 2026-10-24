package com.mouse.mes.service;

import com.mouse.mes.entity.Customer;

import java.util.List;

public interface CustomerService {
    List<Customer> findAll();
    Customer findById(Long id);
    Customer findByCode(String customerCode);
    Customer create(Customer customer);
    Customer update(Customer customer);
    void delete(Long id);
}