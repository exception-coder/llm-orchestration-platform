package com.exceptioncoder.llm.domain.devplan.model;

/**
 * 约束影响 —— 需求变更对现有约束（事务/幂等/鉴权/状态守卫/补偿）的影响评估。
 *
 * @author zhangkai
 * @since 2026-04-12
 */
public record ConstraintImpact(
        String constraintType,  // TRANSACTION / IDEMPOTENT / AUTH / STATE_GUARD / COMPENSATION
        String description,
        String affectedMethod,  // 受影响的方法全路径
        String impact           // NEED_MODIFY / NEED_NEW / NEED_REVIEW
) {
}
