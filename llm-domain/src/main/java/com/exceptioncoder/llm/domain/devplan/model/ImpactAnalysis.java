package com.exceptioncoder.llm.domain.devplan.model;

import java.util.List;

/**
 * 需求影响范围分析
 */
public record ImpactAnalysis(
        List<String> affectedClasses,
        List<String> affectedModules,
        List<String> dependencyChain
) {
}
