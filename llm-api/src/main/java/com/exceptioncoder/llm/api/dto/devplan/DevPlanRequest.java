package com.exceptioncoder.llm.api.dto.devplan;

/**
 * 开发方案生成请求
 */
public record DevPlanRequest(
        String projectPath,
        String requirement,
        String templateType,
        Boolean forceReindex,
        Integer timeoutSeconds
) {
    public DevPlanRequest {
        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException("projectPath 不能为空");
        }
        if (requirement == null || requirement.isBlank()) {
            throw new IllegalArgumentException("requirement 不能为空");
        }
    }

    public int resolvedTimeoutSeconds() {
        return timeoutSeconds != null ? timeoutSeconds : 300;
    }
}
