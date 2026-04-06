package com.exceptioncoder.llm.domain.devplan.model;

import java.util.List;

/**
 * 需求影响范围分析 -- REQUIREMENT_ANALYZER Agent 对需求变更影响面的评估结果。
 *
 * <p>属于 Domain 层 devplan 模块。结合 {@link ProjectStructure} 和 {@link ArchTopology}
 * 信息，分析出需求变更会波及的类、模块以及依赖传播链路，帮助方案设计阶段
 * 精准定位需要修改的代码范围，降低遗漏风险。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public record ImpactAnalysis(
        List<String> affectedClasses,       // 受影响的类全限定名列表
        List<String> affectedModules,       // 受影响的模块名称列表
        List<String> dependencyChain        // 依赖传播链路，从变更源头到最远影响点的有序路径
) {
}
