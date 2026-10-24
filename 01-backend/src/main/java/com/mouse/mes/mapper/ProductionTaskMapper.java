package com.mouse.mes.mapper;

import com.mouse.mes.entity.ProductionTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductionTaskMapper {
    List<ProductionTask> findAll();
    ProductionTask findById(@Param("id") Long id);
    ProductionTask findByTaskNo(@Param("taskNo") String taskNo);
    List<ProductionTask> findByPlanId(@Param("planId") Long planId);
    int insert(ProductionTask productionTask);
    int update(ProductionTask productionTask);
    int delete(@Param("id") Long id);

    /**
     * 仅在任务仍处于待领取状态时完成领取，防止并发重复领取。
     */
    int claim(@Param("id") Long id, @Param("userId") Long userId, @Param("userName") String userName,
              @Param("updateTime") java.util.Date updateTime);

    /**
     * 仅允许任务所属操作工把已领取任务切换为生产中。
     */
    int start(@Param("id") Long id, @Param("userId") Long userId, @Param("startTime") java.util.Date startTime,
              @Param("updateTime") java.util.Date updateTime);
}
