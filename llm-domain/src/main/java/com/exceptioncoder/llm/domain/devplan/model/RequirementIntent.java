package com.exceptioncoder.llm.domain.devplan.model;

import java.util.List;

/**
 * 需求意图分析结果 -- REQUIREMENT_ANALYZER Agent 对用户需求的结构化理解。
 *
 * <p>属于 Domain 层 devplan 模块。将自由文本形式的需求解析为类型、关键词、
 * 复杂度和摘要四个维度，供下游方案设计阶段参考，以生成更有针对性的设计文档。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public record RequirementIntent(
        String type,                // 需求类型，如 NEW_FEATURE / BUG_FIX / REFACTOR / OPTIMIZATION
        List<String> keywords,      // 从需求中提取的关键词列表
        String complexity,          // 复杂度评估，如 LOW / MEDIUM / HIGH
        String summary              // 需求的一句话摘要
) {
}
