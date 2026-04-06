package com.exceptioncoder.llm.domain.devplan.model;

import java.util.Map;

/**
 * Agent 执行输出（通用） -- 封装任意 Agent 单次执行的完整返回信息。
 *
 * <p>属于 Domain 层 devplan 模块。作为 {@link com.exceptioncoder.llm.domain.devplan.service.DevPlanAgentRouter#route}
 * 的统一返回类型，包含原始文本输出、结构化数据以及可观测性指标（Token 消耗、耗时），
 * 便于上层节点将执行结果写入全局状态 {@link DevPlanState} 并记录监控数据。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public record AgentOutput(
        AgentRole role,                         // 执行该输出的 Agent 角色
        String rawOutput,                       // LLM 返回的原始文本
        Map<String, Object> structuredData,     // 从原始文本中解析出的结构化数据（JSON 反序列化后）
        int tokenUsage,                         // 本次调用消耗的 Token 数量
        long elapsedMs                          // 本次调用耗时（毫秒）
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AgentRole role;
        private String rawOutput;
        private Map<String, Object> structuredData = Map.of();
        private int tokenUsage;
        private long elapsedMs;

        public Builder role(AgentRole role) { this.role = role; return this; }
        public Builder rawOutput(String rawOutput) { this.rawOutput = rawOutput; return this; }
        public Builder structuredData(Map<String, Object> structuredData) { this.structuredData = structuredData; return this; }
        public Builder tokenUsage(int tokenUsage) { this.tokenUsage = tokenUsage; return this; }
        public Builder elapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; return this; }

        public AgentOutput build() {
            return new AgentOutput(role, rawOutput, structuredData, tokenUsage, elapsedMs);
        }
    }
}
