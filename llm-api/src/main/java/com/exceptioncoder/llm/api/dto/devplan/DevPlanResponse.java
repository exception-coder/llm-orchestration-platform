package com.exceptioncoder.llm.api.dto.devplan;

import java.util.List;
import java.util.Map;

/**
 * 开发方案生成响应
 */
public record DevPlanResponse(
        String taskId,
        String document,
        ImpactAnalysisVO impactAnalysis,
        ValidationResultVO validationResult,
        MetadataVO metadata
) {
    public record ImpactAnalysisVO(
            List<String> affectedClasses,
            List<String> affectedModules,
            List<String> dependencyChain
    ) {}

    public record ValidationResultVO(
            boolean passed,
            int score,
            List<String> issues
    ) {}

    public record MetadataVO(
            String traceId,
            Map<String, Long> nodeTimings,
            Map<String, Integer> agentTokenUsage,
            long totalElapsedMs,
            int correctionCount
    ) {}
}
