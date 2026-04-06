package com.exceptioncoder.llm.domain.devplan.model;

import java.util.List;

/**
 * 方案验证结果
 */
public record ValidationResult(
        boolean passed,
        int score,
        List<String> issues
) {
    /** 验证通过阈值 */
    public static final int PASS_THRESHOLD = 70;

    public boolean shouldRetry(int currentCorrectionCount, int maxCorrections) {
        return !passed && currentCorrectionCount < maxCorrections;
    }
}
