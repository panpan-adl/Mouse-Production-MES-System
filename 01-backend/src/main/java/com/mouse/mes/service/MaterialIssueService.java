package com.mouse.mes.service;

import com.mouse.mes.entity.MaterialIssue;
import com.mouse.mes.entity.MaterialIssueItem;

import java.util.List;

public interface MaterialIssueService {
    List<MaterialIssue> findAll();
    MaterialIssue findById(Long id);
    MaterialIssue findByIssueNo(String issueNo);
    MaterialIssue create(MaterialIssue materialIssue);
    MaterialIssue update(MaterialIssue materialIssue);
    void delete(Long id);
    List<MaterialIssueItem> findItemsByIssueId(Long issueId);
}