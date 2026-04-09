package com.exceptioncoder.llm.domain.model;

/**
 * 执行步骤明细 -- 单次 Tool 调用的出入参记录。
 *
 * @param id           自增主键（持久化后赋值）
 * @param traceId      关联的流水号
 * @param stepOrder    步骤序号（从 1 开始）
 * @param toolId       工具 ID
 * @param toolName     工具名称
 * @param inputJson    输入参数（JSON）
 * @param outputJson   输出结果（JSON）
 * @param durationMs   耗时(ms)
 * @param success      是否成功
 * @param errorMessage 错误信息
 */
public record ExecutionStep(
        Long id,
        String traceId,
        int stepOrder,
        String toolId,
        String toolName,
        String inputJson,
        String outputJson,
        long durationMs,
        boolean success,
        String errorMessage
) {}
