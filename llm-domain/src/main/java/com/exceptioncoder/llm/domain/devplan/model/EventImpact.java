package com.exceptioncoder.llm.domain.devplan.model;

/**
 * 事件影响 —— 需求变更对事件/消息契约的影响评估。
 *
 * @author zhangkai
 * @since 2026-04-12
 */
public record EventImpact(
        String eventType,   // 事件类型名
        String action,      // NEW / MODIFY / CONSUME
        String topic,       // Topic/Exchange
        String description
) {
}
