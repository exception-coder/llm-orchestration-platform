package com.exceptioncoder.llm.domain.model;

import java.util.Map;
import java.util.Optional;

/**
 * Agent 任务生命周期管理接口。
 *
 * <p>负责任务的提交（含并发控制）、状态查询、完成/失败标记。
 * 实现类位于 Infrastructure 层。</p>
 */
public interface AgentTaskManager {

    /**
     * 异步提交 Agent 执行任务。
     *
     * <p>内部完成并发检查、任务创建、线程池提交。
     * 并发超限时返回 null，调用方据此返回 429。</p>
     *
     * @param agentId        Agent ID
     * @param input          用户输入
     * @param context        上下文变量
     * @param timeoutSeconds 执行超时（秒）
     * @return 初始任务对象（SUBMITTED 状态），并发超限时返回 null
     */
    AgentTask submit(String agentId, String input, Map<String, Object> context, int timeoutSeconds);

    /**
     * 查询任务当前状态。
     */
    Optional<AgentTask> getTask(String executionId);

    /**
     * 标记任务完成。
     */
    void completeTask(String executionId, AgentExecutionResult result);

    /**
     * 标记任务失败。
     */
    void failTask(String executionId, String errorMessage, AgentTask.Status failStatus);
}
