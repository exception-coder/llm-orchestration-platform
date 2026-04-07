package com.exceptioncoder.llm.domain.devplan.model;

import java.util.List;

/**
 * 方案验证结果 -- PLAN_REVIEWER Agent 对设计文档的质量评审输出。
 *
 * <p>属于 Domain 层 devplan 模块。包含是否通过、评分和具体问题列表三个维度，
 * 并提供 {@link #shouldRetry(int, int)} 方法封装"是否需要修正重试"的领域判断逻辑。
 * 评分低于 {@link #PASS_THRESHOLD}（70 分）即判定为不通过。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public record ValidationResult(
        boolean passed,         // 是否通过验证（score >= PASS_THRESHOLD 时为 true）
        int score,              // 质量评分，范围 0-100
        List<String> issues     // 审查发现的具体问题描述列表
) {
    /** 验证通过阈值：评分达到 70 分及以上视为通过 */
    public static final int PASS_THRESHOLD = 70;

    /**
     * 判断是否需要进入修正重试循环。
     *
     * <p>判定逻辑：
     * <ol>
     *   <li>验证未通过（passed == false）</li>
     *   <li>当前修正次数尚未达到最大修正上限</li>
     * </ol>
     * 两个条件同时满足时返回 true，触发重新设计 → 重新审查的修正循环。</p>
     *
     * @param currentCorrectionCount 当前已执行的修正次数
     * @param maxCorrections         允许的最大修正次数
     * @return 若需要重试修正则返回 true，否则返回 false
     */
    public boolean shouldRetry(int currentCorrectionCount, int maxCorrections) {
        // 步骤1：检查验证是否已通过，若已通过则无需重试
        // 步骤2：检查修正次数是否已达上限，未达上限才允许重试
        return !passed && currentCorrectionCount < maxCorrections;
    }
}
