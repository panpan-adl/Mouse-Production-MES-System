package com.mouse.mes.impl;

import com.mouse.mes.entity.Product;
import com.mouse.mes.mapper.ProductMapper;
import com.mouse.mes.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public List<Product> findAll() {
        return productMapper.findAll();
    }

    @Override
    public Product findById(Long id) {
        return productMapper.findById(id);
    }

    @Override
    public Product findByProductCode(String productCode) {
        return productMapper.findByProductCode(productCode);
    }

    @Override
    public Product create(Product product) {
        product.setCreateTime(new Date());
        product.setUpdateTime(new Date());
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        productMapper.insert(product);
        return product;
    }

    @Override
    public Product update(Product product) {
        product.setUpdateTime(new Date());
        productMapper.update(product);
        return productMapper.findById(product.getId());
    }

    @Override
    public void delete(Long id) {
        productMapper.delete(id);
    }
}