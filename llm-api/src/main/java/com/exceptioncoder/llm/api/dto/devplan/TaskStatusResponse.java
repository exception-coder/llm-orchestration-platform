package com.exceptioncoder.llm.api.dto.devplan;

/**
 * 任务状态响应
 */
public record TaskStatusResponse(
        String taskId,
        String status,
        String currentNode,
        long elapsedMs
) {
}
