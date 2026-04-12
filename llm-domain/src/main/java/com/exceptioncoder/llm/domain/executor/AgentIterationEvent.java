package com.exceptioncoder.llm.domain.executor;

/**
 * Agent 迭代事件 —— 领域层事件模型。
 *
 * <p>描述 Agent 执行过程中产生的各类事件，供上层以 {@code Flux<AgentIterationEvent>}
 * 方式消费。不绑定任何传输协议（SSE/WebSocket/gRPC），由 API 层决定如何推送。</p>
 */
public record AgentIterationEvent(
        String executionId,
        Type type,
        int iteration,
        String data
) {
    public enum Type {
        /** LLM 返回一轮思考结果 */
        ITERATION,
        /** 工具执行完成 */
        TOOL_RESULT,
        /** 执行正常结束 */
        COMPLETE,
        /** 执行异常 */
        ERROR,
        /** 执行超时 */
        TIMEOUT
    }

    public static AgentIterationEvent iteration(String executionId, int iteration, String data) {
        return new AgentIterationEvent(executionId, Type.ITERATION, iteration, data);
    }

    public static AgentIterationEvent toolResult(String executionId, int iteration, String data) {
        return new AgentIterationEvent(executionId, Type.TOOL_RESULT, iteration, data);
    }

    public static AgentIterationEvent complete(String executionId, int iteration, String data) {
        return new AgentIterationEvent(executionId, Type.COMPLETE, iteration, data);
    }

    public static AgentIterationEvent error(String executionId, String data) {
        return new AgentIterationEvent(executionId, Type.ERROR, 0, data);
    }
}
