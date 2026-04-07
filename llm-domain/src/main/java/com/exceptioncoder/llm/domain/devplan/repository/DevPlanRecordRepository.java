package com.exceptioncoder.llm.domain.devplan.repository;

/**
 * 方案记录仓储接口 -- 开发方案生成结果的归档持久化契约。
 *
 * <p>属于 Domain 层 devplan 模块。在任务成功完成后，将完整的方案文档、
 * 影响分析、验证评分、性能指标等信息持久化归档，用于后续查询回溯与数据分析。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public interface DevPlanRecordRepository {

    /**
     * 保存一条完整的方案生成记录。
     *
     * @param taskId                任务唯一标识
     * @param projectPath           目标项目路径
     * @param requirement           用户原始需求文本
     * @param document              生成的设计文档全文（Markdown）
     * @param impactAnalysisJson    影响范围分析的 JSON 序列化字符串
     * @param validationScore       验证评分（0-100）
     * @param validationIssuesJson  验证问题列表的 JSON 序列化字符串
     * @param nodeTimingsJson       各节点耗时的 JSON 序列化字符串
     * @param agentTokenUsageJson   各 Agent Token 消耗的 JSON 序列化字符串
     * @param correctionCount       修正循环次数
     * @param modelUsed             使用的 LLM 模型标识
     * @param totalTokenUsage       总 Token 消耗量
     * @param elapsedMs             任务总耗时（毫秒）
     */
    void save(String taskId, String projectPath, String requirement,
              String document, String impactAnalysisJson,
              int validationScore, String validationIssuesJson,
              String nodeTimingsJson, String agentTokenUsageJson,
              int correctionCount, String modelUsed, int totalTokenUsage, long elapsedMs);
}
