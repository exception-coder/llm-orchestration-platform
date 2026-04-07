package com.exceptioncoder.llm.api.dto.devplan;

import java.util.List;
import java.util.Map;

/**
 * 开发方案生成响应 DTO —— 封装方案生成的完整结果。
 *
 * <p>本类属于 <b>API 层（DTO）</b>，是 {@code POST /api/v1/dev-plan/generate} 端点的响应体。
 * 包含方案文档、影响面分析、审查验证结果和流程执行元数据四大部分。</p>
 *
 * <h3>内嵌 VO 说明</h3>
 * <ul>
 *   <li>{@link ImpactAnalysisVO} — 影响面分析视图对象</li>
 *   <li>{@link ValidationResultVO} — 方案审查验证结果视图对象</li>
 *   <li>{@link MetadataVO} — 流程执行元数据视图对象（耗时、Token 用量等）</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public record DevPlanResponse(
        /** 任务唯一标识 */
        String taskId,
        /** 生成的开发方案文档（Markdown 格式） */
        String document,
        /** 影响面分析结果 */
        ImpactAnalysisVO impactAnalysis,
        /** 方案审查验证结果 */
        ValidationResultVO validationResult,
        /** 流程执行元数据 */
        MetadataVO metadata
) {
    /**
     * 影响面分析视图对象 —— 展示需求变更对项目的影响范围。
     *
     * @param affectedClasses  受影响的类列表
     * @param affectedModules  受影响的模块列表
     * @param dependencyChain  依赖链路列表
     */
    public record ImpactAnalysisVO(
            List<String> affectedClasses,
            List<String> affectedModules,
            List<String> dependencyChain
    ) {}

    /**
     * 方案审查验证结果视图对象 —— 展示方案的质量评审结论。
     *
     * @param passed 是否审查通过
     * @param score  审查评分（0-100）
     * @param issues 审查发现的问题列表
     */
    public record ValidationResultVO(
            boolean passed,
            int score,
            List<String> issues
    ) {}

    /**
     * 流程执行元数据视图对象 —— 提供流程运行的可观测性数据。
     *
     * @param traceId          链路追踪 ID（预留，当前为 null）
     * @param nodeTimings      各节点执行耗时（毫秒），key 为节点名称
     * @param agentTokenUsage  各 Agent 的 Token 使用量，key 为 Agent 角色名称
     * @param totalElapsedMs   流程总耗时（毫秒）
     * @param correctionCount  方案修正次数
     */
    public record MetadataVO(
            String traceId,
            Map<String, Long> nodeTimings,
            Map<String, Integer> agentTokenUsage,
            long totalElapsedMs,
            int correctionCount
    ) {}
}
