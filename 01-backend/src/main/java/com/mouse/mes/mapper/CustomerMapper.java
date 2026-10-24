package com.mouse.mes.mapper;

import com.mouse.mes.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerMapper {
    List<Customer> findAll();
    Customer findById(@Param("id") Long id);
    Customer findByCustomerCode(@Param("customerCode") String customerCode);
    int insert(Customer customer);
    int update(Customer customer);
    int delete(@Param("id") Long id);
}
