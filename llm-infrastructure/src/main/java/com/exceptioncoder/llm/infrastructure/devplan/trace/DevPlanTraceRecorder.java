package com.exceptioncoder.llm.infrastructure.devplan.trace;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全链路 Trace 记录器 -- 管理 Span 的创建、嵌套和生命周期。
 *
 * <p>采用 ThreadLocal Span 栈实现父子关系自动关联，
 * 内存 + SLF4J 结构化日志输出。一期轻量实现，后续可接入 OpenTelemetry。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
public class DevPlanTraceRecorder {

    private final ThreadLocal<Deque<SpanContext>> spanStack = ThreadLocal.withInitial(ArrayDeque::new);
    private final Map<String, List<SpanContext>> traceSpans = new ConcurrentHashMap<>();

    /**
     * 创建新的 Trace ID（任务级别，一个任务一个 traceId）。
     */
    public String createTrace() {
        String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        traceSpans.put(traceId, Collections.synchronizedList(new ArrayList<>()));
        log.info("[TRACE] 创建 Trace: traceId={}", traceId);
        return traceId;
    }

    /**
     * 开始一个 Span，自动关联父 Span。
     *
     * @param traceId    所属 Trace ID
     * @param name       Span 名称（如 "ScanNode"、"devplan_project_scan"）
     * @param attributes 附加属性
     * @return 新创建的 SpanContext
     */
    public SpanContext startSpan(String traceId, String name, Map<String, String> attributes) {
        Deque<SpanContext> stack = spanStack.get();
        String parentSpanId = stack.isEmpty() ? null : stack.peek().spanId();
        String spanId = "span-" + UUID.randomUUID().toString().substring(0, 8);

        SpanContext span = new SpanContext(
                traceId, spanId, parentSpanId, name,
                System.currentTimeMillis(),
                attributes != null ? attributes : Map.of()
        );

        stack.push(span);
        log.info("[TRACE] 开始 Span: traceId={} spanId={} parentSpanId={} name={}",
                traceId, spanId, parentSpanId, name);
        return span;
    }

    /**
     * 结束 Span，记录耗时并出栈。
     */
    public void endSpan(SpanContext span) {
        Deque<SpanContext> stack = spanStack.get();
        if (!stack.isEmpty() && stack.peek().spanId().equals(span.spanId())) {
            stack.pop();
        }

        long elapsedMs = span.elapsedMs();
        List<SpanContext> spans = traceSpans.get(span.traceId());
        if (spans != null) {
            spans.add(span);
        }

        log.info("[TRACE] 结束 Span: traceId={} spanId={} name={} elapsedMs={}",
                span.traceId(), span.spanId(), span.name(), elapsedMs);
    }

    /**
     * 获取指定 Trace 的所有已结束 Span。
     */
    public List<SpanContext> getSpans(String traceId) {
        return traceSpans.getOrDefault(traceId, List.of());
    }

    /**
     * 清理 Trace 数据（任务完成后调用，防止内存泄漏）。
     */
    public void cleanupTrace(String traceId) {
        traceSpans.remove(traceId);
        spanStack.remove();
    }
}
