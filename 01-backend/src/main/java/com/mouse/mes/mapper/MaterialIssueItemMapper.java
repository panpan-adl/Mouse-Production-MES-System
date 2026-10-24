package com.mouse.mes.mapper;

import com.mouse.mes.entity.MaterialIssueItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MaterialIssueItemMapper {
    List<MaterialIssueItem> findByIssueId(@Param("issueId") Long issueId);
    int insert(MaterialIssueItem materialIssueItem);
    int deleteByIssueId(@Param("issueId") Long issueId);
}
