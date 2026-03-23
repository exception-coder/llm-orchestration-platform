package com.exceptioncoder.llm.domain.model;

/**
 * 工具类型
 */
public enum ToolType {
    /** 函数调用型工具 */
    FUNCTION,
    /** 代码解释器工具 */
    CODE_INTERPRETER,
    /** 检索型工具（向量搜索/知识库） */
    RETRIEVER,
    /** 网页搜索工具 */
    WEB_SEARCH,
    /** 计算器工具 */
    CALCULATOR,
    /** 自定义工具 */
    CUSTOM
}
