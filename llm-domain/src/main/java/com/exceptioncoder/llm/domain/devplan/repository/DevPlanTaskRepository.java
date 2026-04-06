package com.exceptioncoder.llm.domain.devplan.repository;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanTask;

import java.util.Optional;

/**
 * 任务仓储接口
 */
public interface DevPlanTaskRepository {

    DevPlanTask save(DevPlanTask task);

    Optional<DevPlanTask> findByTaskId(String taskId);

    void updateStatus(String taskId, String status, String currentNode);
}
