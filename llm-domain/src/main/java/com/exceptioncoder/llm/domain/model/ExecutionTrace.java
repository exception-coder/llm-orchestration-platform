package com.exceptioncoder.llm.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行流水主记录 -- 一次 Agent 或 Tool 的完整执行记录。
 *
 * @param traceId      唯一流水号（= AgentExecutionResult.executionId）
 * @param agentId      执行的 Agent ID（Tool 直接调用时为 null）
 * @param agentName    Agent 名称
 * @param userInput    用户输入
 * @param finalOutput  最终输出
 * @param status       执行状态
 * @param iterations   迭代次数
 * @param elapsedMs    总耗时(ms)
 * @param errorMessage 错误信息
 * @param steps        执行步骤明细（Tool 调用记录）
 * @param createdAt    创建时间
 */
public record ExecutionTrace(
        String traceId,
        String agentId,
        String agentName,
        String userInput,
        String finalOutput,
        String status,
        int iterations,
        long elapsedMs,
        String errorMessage,
        List<ExecutionStep> steps,
        LocalDateTime createdAt
) {

    /**
     * 从 AgentExecutionResult 构建 ExecutionTrace。
     */
    public static ExecutionTrace fromResult(AgentExecutionResult result, String agentName, String userInput) {
        List<ExecutionStep> steps = List.of();
        if (result.toolCalls() != null) {
            steps = new java.util.ArrayList<>();
            for (int i = 0; i < result.toolCalls().size(); i++) {
                ToolCall tc = result.toolCalls().get(i);
                steps.add(new ExecutionStep(
                        null, result.executionId(), i + 1,
                        tc.toolId(), tc.toolName(),
                        tc.inputJson(), tc.output(),
                        tc.durationMs(), tc.success(), tc.errorMessage()
                ));
            }
        }
        return new ExecutionTrace(
                result.executionId(), result.agentId(), agentName,
                userInput, result.finalOutput(),
                result.status().name(), result.iterations(),
                result.elapsedMs(), result.errorMessage(),
                steps, LocalDateTime.now()
        );
    }

    /**
     * 为单个 Tool 直接调用构建 ExecutionTrace。
     */
    public static ExecutionTrace forToolCall(String traceId, String toolId, String toolName,
                                             String inputJson, String output,
                                             long durationMs, boolean success, String errorMessage) {
        ExecutionStep step = new ExecutionStep(
                null, traceId, 1, toolId, toolName,
                inputJson, output, durationMs, success, errorMessage
        );
        return new ExecutionTrace(
                traceId, null, null, inputJson, output,
                success ? "SUCCESS" : "FAILED", 1, durationMs, errorMessage,
                List.of(step), LocalDateTime.now()
        );
    }
}
