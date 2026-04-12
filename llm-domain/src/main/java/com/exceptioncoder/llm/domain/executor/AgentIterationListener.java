package com.exceptioncoder.llm.domain.executor;

import com.exceptioncoder.llm.domain.model.AgentExecutionResult;
import com.exceptioncoder.llm.domain.model.ToolCall;

/**
 * Agent 迭代级事件回调接口。
 *
 * <p>供 AgentExecutor 在 ReAct 循环的每轮迭代后通知外部观察者
 * （如 SSE 推送、日志记录等）。所有方法均为非阻塞调用，
 * 实现方不应抛出异常影响主执行流程。</p>
 */
public interface AgentIterationListener {

    /**
     * 每轮 ReAct 迭代 LLM 返回后回调。
     */
    void onIteration(String executionId, int iteration, String thought, ToolCall toolCall);

    /**
     * 工具执行完成后回调。
     */
    void onToolResult(String executionId, int iteration, String toolName, String output);

    /**
     * 执行正常结束回调。
     */
    void onComplete(String executionId, AgentExecutionResult result);

    /**
     * 执行异常回调。
     */
    void onError(String executionId, String errorMessage);

    /**
     * 空实现，用于不需要回调的场景。
     */
    AgentIterationListener NOOP = new AgentIterationListener() {
        @Override public void onIteration(String executionId, int iteration, String thought, ToolCall toolCall) {}
        @Override public void onToolResult(String executionId, int iteration, String toolName, String output) {}
        @Override public void onComplete(String executionId, AgentExecutionResult result) {}
        @Override public void onError(String executionId, String errorMessage) {}
    };
}
