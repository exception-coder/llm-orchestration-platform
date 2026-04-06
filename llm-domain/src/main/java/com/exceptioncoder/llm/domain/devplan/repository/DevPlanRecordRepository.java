package com.exceptioncoder.llm.domain.devplan.repository;

/**
 * 方案记录仓储接口
 */
public interface DevPlanRecordRepository {

    void save(String taskId, String projectPath, String requirement,
              String document, String impactAnalysisJson,
              int validationScore, String validationIssuesJson,
              String nodeTimingsJson, String agentTokenUsageJson,
              int correctionCount, String modelUsed, int totalTokenUsage, long elapsedMs);
}
