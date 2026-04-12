package com.exceptioncoder.llm.domain.devplan.model;

/**
 * 跨服务影响 —— 需求变更对下游服务调用的影响评估。
 *
 * @author zhangkai
 * @since 2026-04-12
 */
public record CrossServiceImpact(
        String targetService,    // 目标服务注册名称
        String callType,         // SYNC_CALL / ASYNC_EVENT
        String currentInterface, // 当前接口（如有）
        String requiredChange    // 需要的变更描述
) {
}
