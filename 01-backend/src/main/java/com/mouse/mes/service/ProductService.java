package com.mouse.mes.service;

import com.mouse.mes.entity.Product;

import java.util.List;

public interface ProductService {
    List<Product> findAll();
    Product findById(Long id);
    Product findByProductCode(String productCode);
    Product create(Product product);
    Product update(Product product);
    void delete(Long id);
}