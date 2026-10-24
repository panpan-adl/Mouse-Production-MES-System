package com.mouse.mes.impl;

import com.mouse.mes.entity.MaterialIssue;
import com.mouse.mes.entity.MaterialIssueItem;
import com.mouse.mes.mapper.MaterialIssueItemMapper;
import com.mouse.mes.mapper.MaterialIssueMapper;
import com.mouse.mes.service.MaterialIssueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class MaterialIssueServiceImpl implements MaterialIssueService {

    @Autowired
    private MaterialIssueMapper materialIssueMapper;

    @Autowired
    private MaterialIssueItemMapper materialIssueItemMapper;

    @Override
    public List<MaterialIssue> findAll() {
        return materialIssueMapper.findAll();
    }

    @Override
    public MaterialIssue findById(Long id) {
        return materialIssueMapper.findById(id);
    }

    @Override
    public MaterialIssue findByIssueNo(String issueNo) {
        return materialIssueMapper.findByIssueNo(issueNo);
    }

    @Override
    public MaterialIssue create(MaterialIssue materialIssue) {
        materialIssue.setCreatedAt(new Date());
        materialIssue.setUpdatedAt(new Date());
        materialIssue.setIsDeleted(0);
        materialIssueMapper.insert(materialIssue);
        return materialIssue;
    }

    @Override
    public MaterialIssue update(MaterialIssue materialIssue) {
        materialIssue.setUpdatedAt(new Date());
        materialIssueMapper.update(materialIssue);
        return materialIssueMapper.findById(materialIssue.getId());
    }

    @Override
    public void delete(Long id) {
        materialIssueMapper.delete(id);
    }

    @Override
    public List<MaterialIssueItem> findItemsByIssueId(Long issueId) {
        return materialIssueItemMapper.findByIssueId(issueId);
    }
}