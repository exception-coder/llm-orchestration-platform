package com.exceptioncoder.llm.domain.executor;

import reactor.core.publisher.Flux;

/**
 * Agent 事件发布接口 —— 领域层定义，基础设施层实现。
 *
 * <p>提供基于 Reactor Flux 的事件流订阅能力。
 * 上层（Application / API）通过此接口获取事件流，
 * 不需要感知底层的 Sinks、SseEmitter 等技术细节。</p>
 */
public interface AgentEventPublisher {

    /**
     * 获取指定执行任务的事件流。
     *
     * <p>返回的 Flux 是热流（hot stream），多个订阅者共享同一事件序列。
     * 执行完成或失败后自动 complete。</p>
     *
     * @param executionId 执行唯一标识
     * @return 事件流，若 executionId 不存在则返回空 Flux
     */
    Flux<AgentIterationEvent> getEventStream(String executionId);
}
