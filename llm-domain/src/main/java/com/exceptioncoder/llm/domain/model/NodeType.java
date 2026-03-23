package com.exceptioncoder.llm.domain.model;

/**
 * Graph 节点类型
 */
public enum NodeType {
    /** LLM 调用节点 */
    LLM,
    /** 工具调用节点 */
    TOOL,
    /** 条件分支节点 */
    CONDITION,
    /** 合并节点 */
    MERGE,
    /** 并行执行节点 */
    PARALLEL,
    /** 循环节点 */
    LOOP,
    /** 输出节点 */
    OUTPUT
}
