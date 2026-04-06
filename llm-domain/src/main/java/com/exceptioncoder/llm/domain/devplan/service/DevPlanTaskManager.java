package com.exceptioncoder.llm.domain.devplan.service;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanTask;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * 任务管理接口 — 控制平面核心契约
 */
public interface DevPlanTaskManager {

    /**
     * 创建并提交任务（含并发检查）
     */
    DevPlanTask submitTask(String projectPath, String requirement, int timeoutSeconds);

    /**
     * 查询任务状态
     */
    Optional<DevPlanTask> getTask(String taskId);

    /**
     * 标记任务完成
     */
    void completeTask(String taskId);

    /**
     * 标记任务失败
     */
    void failTask(String taskId, String errorMessage);

    /**
     * 带超时执行
     */
    <T> T executeWithTimeout(Callable<T> callable, int timeoutSeconds);
}
