package com.exceptioncoder.llm.application.devplan;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.model.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * 审查条件路由策略 —— 决定 ReviewNode 执行后的流程走向。
 *
 * <p>本类属于 <b>应用层（Application Layer）</b>，是 devplan 模块中
 * DesignNode ⇄ ReviewNode 修正循环的路由决策点。
 * 它根据审查评分、是否通过以及已修正次数，输出三种决策之一：</p>
 * <ul>
 *   <li>{@link #APPROVED} — 审查通过，流程正常结束</li>
 *   <li>{@link #RETRY_DESIGN} — 审查不通过且修正次数未超限，回退到 DesignNode 重新生成</li>
 *   <li>{@link #APPROVED_WITH_ISSUES} — 修正次数超限或无法获取审查结果，带问题强制通过</li>
 * </ul>
 *
 * <h3>设计思路</h3>
 * <p>将路由决策从 {@link DevPlanFlowDefinition} 中抽离为独立策略类，
 * 遵循单一职责原则，便于后续扩展更复杂的路由规则（如根据问题严重级别分级处理）。</p>
 *
 * <h3>协作关系</h3>
 * <ul>
 *   <li>{@link DevPlanFlowDefinition} — 在修正循环中调用本策略获取路由决策</li>
 *   <li>{@link ValidationResult} — 审查结果值对象，提供评分和通过判定</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Component
public class ReviewRoutingStrategy {

    /** 路由决策：审查通过，流程正常结束 */
    public static final String APPROVED = "APPROVED";
    /** 路由决策：审查不通过，回退到 DesignNode 重新生成方案 */
    public static final String RETRY_DESIGN = "RETRY_DESIGN";
    /** 路由决策：修正次数超限或无审查结果，带问题强制通过 */
    public static final String APPROVED_WITH_ISSUES = "APPROVED_WITH_ISSUES";

    /** 最大允许修正次数，超过此值将强制带问题通过 */
    private static final int MAX_CORRECTIONS = 3;

    /**
     * 根据当前流程状态执行路由决策。
     *
     * <p>决策逻辑优先级：</p>
     * <ol>
     *   <li>若审查结果为 null，返回 {@link #APPROVED_WITH_ISSUES}（容错处理）</li>
     *   <li>若审查通过或评分达到阈值，返回 {@link #APPROVED}</li>
     *   <li>若审查不通过且修正次数未超限，返回 {@link #RETRY_DESIGN}</li>
     *   <li>其他情况返回 {@link #APPROVED_WITH_ISSUES}（兜底策略）</li>
     * </ol>
     *
     * @param state 当前流程状态，包含审查结果 {@code validation} 和修正计数 {@code correctionCount}
     * @return 路由决策字符串，取值为 {@link #APPROVED}、{@link #RETRY_DESIGN} 或 {@link #APPROVED_WITH_ISSUES}
     */
    public String route(DevPlanState state) {
        ValidationResult validation = state.validation();

        // 容错：审查结果缺失，视为带问题通过（避免无限循环）
        if (validation == null) {
            return APPROVED_WITH_ISSUES;
        }

        // 审查通过或评分达到阈值，流程正常结束
        if (validation.passed() || validation.score() >= ValidationResult.PASS_THRESHOLD) {
            return APPROVED;
        }

        // 审查不通过且修正次数未超限，回退到 DesignNode 重新生成
        if (validation.shouldRetry(state.correctionCount(), MAX_CORRECTIONS)) {
            return RETRY_DESIGN;
        }

        // 兜底：修正次数已超限，强制带问题通过
        return APPROVED_WITH_ISSUES;
    }
}
