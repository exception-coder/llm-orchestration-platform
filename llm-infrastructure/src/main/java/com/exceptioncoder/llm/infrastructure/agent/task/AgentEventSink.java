package com.exceptioncoder.llm.infrastructure.agent.task;

import com.exceptioncoder.llm.domain.executor.AgentEventPublisher;
import com.exceptioncoder.llm.domain.executor.AgentIterationEvent;
import com.exceptioncoder.llm.domain.executor.AgentIterationListener;
import com.exceptioncoder.llm.domain.model.AgentExecutionResult;
import com.exceptioncoder.llm.domain.model.ToolCall;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 事件汇聚器 —— 同时实现 Listener（写入端）和 Publisher（读取端）。
 *
 * <p>每个 executionId 对应一个 {@link Sinks.Many}，实现事件的发布-订阅。
 * 不绑定任何传输协议，上层消费 {@code Flux<AgentIterationEvent>} 后
 * 自行决定通过 SSE / WebSocket / gRPC 推送给客户端。</p>
 */
@Slf4j
@Component
public class AgentEventSink implements AgentIterationListener, AgentEventPublisher {

    private final ConcurrentHashMap<String, Sinks.Many<AgentIterationEvent>> sinks = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Flux<AgentIterationEvent> getEventStream(String executionId) {
        Sinks.Many<AgentIterationEvent> sink = sinks.computeIfAbsent(executionId,
                k -> Sinks.many().multicast().onBackpressureBuffer());
        return sink.asFlux();
    }

    @Override
    public void onIteration(String executionId, int iteration, String thought, ToolCall toolCall) {
        String data = toJson(Map.of(
                "iteration", iteration,
                "thought", thought != null ? thought : "",
                "toolCall", toolCall != null ? Map.of(
                        "toolName", toolCall.toolName(),
                        "input", toolCall.inputJson() != null ? toolCall.inputJson() : ""
                ) : Map.of()
        ));
        emit(executionId, AgentIterationEvent.iteration(executionId, iteration, data));
    }

    @Override
    public void onToolResult(String executionId, int iteration, String toolName, String output) {
        String data = toJson(Map.of(
                "iteration", iteration,
                "toolName", toolName,
                "output", output != null ? output : ""
        ));
        emit(executionId, AgentIterationEvent.toolResult(executionId, iteration, data));
    }

    @Override
    public void onComplete(String executionId, AgentExecutionResult result) {
        String data = toJson(Map.of(
                "executionId", executionId,
                "finalOutput", result.finalOutput() != null ? result.finalOutput() : "",
                "iterations", result.iterations()
        ));
        emit(executionId, AgentIterationEvent.complete(executionId, result.iterations(), data));
        completeSink(executionId);
    }

    @Override
    public void onError(String executionId, String errorMessage) {
        String data = toJson(Map.of(
                "executionId", executionId,
                "errorMessage", errorMessage != null ? errorMessage : "未知错误"
        ));
        emit(executionId, AgentIterationEvent.error(executionId, data));
        completeSink(executionId);
    }

    private void emit(String executionId, AgentIterationEvent event) {
        Sinks.Many<AgentIterationEvent> sink = sinks.get(executionId);
        if (sink == null) return;

        Sinks.EmitResult result = sink.tryEmitNext(event);
        if (result.isFailure()) {
            log.debug("事件发送失败: executionId={}, result={}", executionId, result);
        }
    }

    private void completeSink(String executionId) {
        Sinks.Many<AgentIterationEvent> sink = sinks.remove(executionId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON 序列化失败", e);
            return "{}";
        }
    }
}
