package com.exceptioncoder.llm.domain.devplan.model;

/**
 * Agent 角色枚举
 * 定义开发方案生成流程中各角色 Agent 的职责
 */
public enum AgentRole {

    /** 代码感知：扫描项目结构、索引代码、提取架构拓扑 */
    CODE_AWARENESS,

    /** 需求分析：识别意图、分析影响范围、评估依赖 */
    REQUIREMENT_ANALYZER,

    /** 方案设计：按模板逐章节生成设计文档 */
    SOLUTION_ARCHITECT,

    /** 方案审查：架构合规、命名规范、LLM-as-Judge 质量评审 */
    PLAN_REVIEWER
}
