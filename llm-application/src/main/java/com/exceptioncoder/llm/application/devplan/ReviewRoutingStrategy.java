package com.exceptioncoder.llm.application.devplan;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.model.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * 审查条件路由策略
 * 决定审查后流程走向：通过 / 重试 / 带问题通过
 */
@Component
public class ReviewRoutingStrategy {

    public static final String APPROVED = "APPROVED";
    public static final String RETRY_DESIGN = "RETRY_DESIGN";
    public static final String APPROVED_WITH_ISSUES = "APPROVED_WITH_ISSUES";

    private static final int MAX_CORRECTIONS = 3;

    /**
     * 路由决策
     */
    public String route(DevPlanState state) {
        ValidationResult validation = state.validation();
        if (validation == null) {
            return APPROVED_WITH_ISSUES;
        }

        if (validation.passed() || validation.score() >= ValidationResult.PASS_THRESHOLD) {
            return APPROVED;
        }

        if (validation.shouldRetry(state.correctionCount(), MAX_CORRECTIONS)) {
            return RETRY_DESIGN;
        }

        return APPROVED_WITH_ISSUES;
    }
}
