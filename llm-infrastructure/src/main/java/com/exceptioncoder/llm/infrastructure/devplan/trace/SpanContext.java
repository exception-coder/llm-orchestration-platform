package com.exceptioncoder.llm.infrastructure.devplan.trace;

import java.util.Map;

/**
 * Span 上下文 -- 全链路追踪中单个 Span 的不可变快照。
 *
 * <p>记录一次 Node / Agent / Tool 调用的起止时间、层级关系和附加属性，
 * 由 {@link DevPlanTraceRecorder} 管理生命周期。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
public record SpanContext(
        String traceId,
        String spanId,
        String parentSpanId,
        String name,
        long startTimeMs,
        Map<String, String> attributes
) {

    /**
     * 创建一个标记了结束时间的 Span 副本（用于日志输出）。
     */
    public long elapsedMs() {
        return System.currentTimeMillis() - startTimeMs;
    }
}
