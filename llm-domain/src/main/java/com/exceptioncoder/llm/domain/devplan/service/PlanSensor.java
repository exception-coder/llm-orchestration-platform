package com.exceptioncoder.llm.domain.devplan.service;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanDocument;
import com.exceptioncoder.llm.domain.devplan.model.ValidationResult;

/**
 * 方案验证传感器接口 -- 对生成的设计文档执行质量评审与合规检查。
 *
 * <p>属于 Domain 层 devplan 模块。命名中的"传感器"借鉴控制论概念：在反馈
 * 修正循环中充当感知器角色，检测方案质量是否达标，为图编排的条件分支
 * 提供决策依据（通过 → 结束，未通过 → 进入修正循环）。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public interface PlanSensor {

    /**
     * 验证设计文档的质量，返回评审结果。
     *
     * <p>实现类通常调用 PLAN_REVIEWER Agent 或 LLM-as-Judge 模式对文档
     * 进行架构合规性、命名规范、章节完整性等维度的打分与问题检测。</p>
     *
     * @param document 待验证的设计文档
     * @return 验证结果，包含是否通过、评分和具体问题列表
     */
    ValidationResult validate(DevPlanDocument document);
}
