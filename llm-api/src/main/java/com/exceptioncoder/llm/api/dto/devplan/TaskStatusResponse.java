package com.exceptioncoder.llm.api.dto.devplan;

/**
 * 任务状态响应 DTO —— 封装任务的实时运行状态。
 *
 * <p>本类属于 <b>API 层（DTO）</b>，是 {@code GET /api/v1/dev-plan/task/{taskId}} 端点的响应体。
 * 提供任务的基本状态信息，用于前端轮询展示任务进度。</p>
 *
 * @param taskId      任务唯一标识
 * @param status      任务当前状态（如 RUNNING、COMPLETED、FAILED）
 * @param currentNode 当前正在执行的节点名称（如 scan、analyze、design、review）
 * @param elapsedMs   任务已运行时长（毫秒）
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public record TaskStatusResponse(
        String taskId,
        String status,
        String currentNode,
        long elapsedMs
) {
}
