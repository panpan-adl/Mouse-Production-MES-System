package com.mouse.mes.impl;

import com.mouse.mes.entity.Supplier;
import com.mouse.mes.mapper.SupplierMapper;
import com.mouse.mes.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class SupplierServiceImpl implements SupplierService {

    /**
     * 供应商数据库访问对象。
     */
    @Autowired
    private SupplierMapper supplierMapper;

    /**
     * 查询全部未删除供应商，活跃供应商排在禁用供应商前面。
     */
    @Override
    public List<Supplier> findAll() {
        return supplierMapper.findAll();
    }

    /**
     * 按供应商ID查询未删除供应商。
     */
    @Override
    public Supplier findById(Long id) {
        return supplierMapper.findById(id);
    }

    /**
     * 按供应商编码查询未删除供应商。
     */
    @Override
    public Supplier findBySupplierCode(String supplierCode) {
        return supplierMapper.findBySupplierCode(supplierCode);
    }

    /**
     * 按供应商编码、名称和状态组合查询供应商；状态为空时查询全部未删除供应商。
     */
    @Override
    public List<Supplier> search(String supplierCode, String supplierName, Integer status) {
        validateSupplierStatus(status);
        return supplierMapper.search(blankToNull(supplierCode), blankToNull(supplierName), status);
    }

    /**
     * 新增供应商，供应商编码不能重复。
     */
    @Override
    public Supplier create(Supplier supplier) {
        validateSupplierBase(supplier);
        validateSupplierStatus(supplier.getStatus());
        if (supplierMapper.findBySupplierCode(supplier.getSupplierCode()) != null) {
            throw new IllegalArgumentException("供应商编码已存在");
        }
        supplier.setCreateTime(new Date());
        supplier.setUpdateTime(new Date());
        if (supplier.getStatus() == null) {
            supplier.setStatus(1);
        }
        supplierMapper.insert(supplier);
        return supplierMapper.findById(supplier.getId());
    }

    /**
     * 修改供应商基础信息。
     */
    @Override
    public Supplier update(Supplier supplier) {
        if (supplier == null || supplier.getId() == null) {
            throw new IllegalArgumentException("供应商ID不能为空");
        }
        Supplier existing = supplierMapper.findById(supplier.getId());
        if (existing == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        mergeMissingFields(supplier, existing);
        validateSupplierBase(supplier);
        validateSupplierStatus(supplier.getStatus());
        supplier.setUpdateTime(new Date());
        supplierMapper.update(supplier);
        return supplierMapper.findById(supplier.getId());
    }

    /**
     * 删除供应商；删除后不再出现在普通查询中。
     */
    @Override
    public void delete(Long id) {
        supplierMapper.delete(id);
    }

    /**
     * 禁用供应商，禁用后该供应商不能再用于创建采购订单。
     */
    @Override
    public Supplier disable(Long id) {
        Supplier existing = supplierMapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        supplierMapper.disable(id);
        existing.setStatus(0);
        existing.setUpdateTime(new Date());
        return existing;
    }

    /**
     * 启用供应商，用于恢复被禁用的供应商。
     */
    @Override
    public Supplier enable(Long id) {
        Supplier existing = supplierMapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        supplierMapper.enable(id);
        existing.setStatus(1);
        existing.setUpdateTime(new Date());
        return existing;
    }

    /**
     * 校验供应商编码和名称等基础字段。
     */
    private void validateSupplierBase(Supplier supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不能为空");
        }
        if (supplier.getSupplierCode() == null || supplier.getSupplierCode().isBlank()) {
            throw new IllegalArgumentException("供应商编码不能为空");
        }
        if (supplier.getSupplierName() == null || supplier.getSupplierName().isBlank()) {
            throw new IllegalArgumentException("供应商名称不能为空");
        }
    }

    /**
     * 校验供应商状态；业务上 1 表示活跃，0 表示禁用。
     */
    private void validateSupplierStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new IllegalArgumentException("供应商状态不合法");
        }
    }

    /**
     * 将空白字符串转换为 null，方便 Mapper 做动态条件查询。
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 修改供应商时，将请求中未传的字段用原供应商字段补齐，避免空值覆盖。
     */
    private void mergeMissingFields(Supplier target, Supplier existing) {
        if (target.getSupplierCode() == null || target.getSupplierCode().isBlank()) {
            target.setSupplierCode(existing.getSupplierCode());
        }
        if (target.getSupplierName() == null || target.getSupplierName().isBlank()) {
            target.setSupplierName(existing.getSupplierName());
        }
        if (target.getContactPerson() == null) {
            target.setContactPerson(existing.getContactPerson());
        }
        if (target.getContactPhone() == null) {
            target.setContactPhone(existing.getContactPhone());
        }
        if (target.getAddress() == null) {
            target.setAddress(existing.getAddress());
        }
        if (target.getBankName() == null) {
            target.setBankName(existing.getBankName());
        }
        if (target.getBankAccount() == null) {
            target.setBankAccount(existing.getBankAccount());
        }
        if (target.getStatus() == null) {
            target.setStatus(existing.getStatus());
        }
        if (target.getRemark() == null) {
            target.setRemark(existing.getRemark());
        }
        target.setCreateTime(existing.getCreateTime());
    }
}
