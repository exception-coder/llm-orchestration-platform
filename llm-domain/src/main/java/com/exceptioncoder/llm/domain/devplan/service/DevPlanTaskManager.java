package com.exceptioncoder.llm.domain.devplan.service;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanTask;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * 任务管理接口 -- 开发方案生成流程的控制平面核心契约。
 *
 * <p>属于 Domain 层 devplan 模块。定义任务提交、状态查询、生命周期管理
 * 以及超时控制等能力，由 Application 层编排调用。实现类负责处理并发冲突检测、
 * 状态机流转以及超时中断等技术细节。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public interface DevPlanTaskManager {

    /**
     * 创建并提交开发方案任务。
     *
     * <p>实现时应检查同一项目是否已有运行中的任务（并发检查），
     * 若存在则抛出异常或返回已有任务。</p>
     *
     * @param projectPath    目标项目的本地路径
     * @param requirement    用户输入的原始需求文本
     * @param timeoutSeconds 任务超时时间（秒）
     * @return 创建成功的任务实体
     * @throws IllegalStateException 若同一项目已有运行中的任务
     */
    DevPlanTask submitTask(String projectPath, String requirement, int timeoutSeconds);

    /**
     * 根据任务 ID 查询任务当前状态。
     *
     * @param taskId 任务唯一标识
     * @return 任务实体的 Optional 包装，不存在时返回 empty
     */
    Optional<DevPlanTask> getTask(String taskId);

    /**
     * 将指定任务标记为已完成。
     *
     * @param taskId 任务唯一标识
     */
    void completeTask(String taskId);

    /**
     * 将指定任务标记为失败，并记录错误信息。
     *
     * @param taskId       任务唯一标识
     * @param errorMessage 失败原因描述
     */
    void failTask(String taskId, String errorMessage);

    /**
     * 带超时保护地执行给定的可调用任务。
     *
     * <p>若执行时间超过指定秒数，应中断执行并抛出超时异常。</p>
     *
     * @param callable       待执行的可调用任务
     * @param timeoutSeconds 超时时间（秒）
     * @param <T>            返回值类型
     * @return 可调用任务的执行结果
     * @throws java.util.concurrent.TimeoutException 若执行超时
     */
    <T> T executeWithTimeout(Callable<T> callable, int timeoutSeconds);
}
