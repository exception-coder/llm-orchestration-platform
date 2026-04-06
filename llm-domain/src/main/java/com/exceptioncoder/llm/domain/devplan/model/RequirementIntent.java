package com.exceptioncoder.llm.domain.devplan.model;

import java.util.List;

/**
 * 需求意图分析结果
 */
public record RequirementIntent(
        String type,
        List<String> keywords,
        String complexity,
        String summary
) {
}
