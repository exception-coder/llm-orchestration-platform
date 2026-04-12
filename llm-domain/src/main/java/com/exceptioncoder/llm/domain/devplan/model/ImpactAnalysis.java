package com.exceptioncoder.llm.domain.devplan.model;

import java.util.List;

/**
 * 需求影响范围分析 —— REQUIREMENT_ANALYZER Agent 的结构化输出。
 *
 * <p>v1 只有 affectedClasses/affectedModules/dependencyChain 三个简单字符串列表。
 * v2 扩展为完整的影响分析结构，新增约束影响、跨服务影响、事件影响三个维度，
 * 为下游 SolutionArchitect 提供更精准的设计输入。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public record ImpactAnalysis(
        String requirementType,                     // CRUD / INTEGRATION / REFACTOR / NEW_DOMAIN / ENHANCEMENT / CROSS_CUTTING
        String requirementSummary,                  // 一句话需求摘要
        boolean degraded,                           // 是否降级模式（business-context.md 不可用时为 true）
        List<String> affectedModules,               // 受影响的模块名称列表
        List<AffectedClass> affectedClasses,        // 受影响的类（v2 升级为结构化对象）
        List<ConstraintImpact> constraintImpacts,   // 约束影响（v2 新增）
        List<CrossServiceImpact> crossServiceImpacts, // 跨服务影响（v2 新增）
        List<EventImpact> eventImpacts,             // 事件影响（v2 新增）
        List<String> dependencyChain,               // 依赖传播链路
        List<ReusableComponent> reusableComponents, // 可复用的现有组件
        List<RiskPoint> riskPoints,                 // 风险点
        List<NewClassSuggestion> newClassesNeeded   // 建议新建的类
) {

    /**
     * 受影响的类（v2 结构化，含证据来源）。
     */
    public record AffectedClass(
            String fullClassName,
            String filePath,
            String impact,      // MODIFY / REUSE / NEW
            String reason,
            String evidence     // 信息来源：business-context.md 维度 N / CodeSearchTool
    ) {}

    /**
     * 可复用的现有组件。
     */
    public record ReusableComponent(
            String fullClassName,
            String reusePlan
    ) {}

    /**
     * 风险点。
     */
    public record RiskPoint(
            String risk,
            String severity,    // HIGH / MEDIUM / LOW
            String mitigation
    ) {}

    /**
     * 建议新建的类。
     */
    public record NewClassSuggestion(
            String suggestedFullClassName,
            String layer,       // domain / application / infrastructure / api
            String purpose
    ) {}
}
