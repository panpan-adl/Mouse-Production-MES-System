package com.mouse.mes.mapper;

import com.mouse.mes.entity.MaterialIssue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MaterialIssueMapper {
    List<MaterialIssue> findAll();
    MaterialIssue findById(@Param("id") Long id);
    MaterialIssue findByIssueNo(@Param("issueNo") String issueNo);
    int insert(MaterialIssue materialIssue);
    int update(MaterialIssue materialIssue);
    int delete(@Param("id") Long id);
}
