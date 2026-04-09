package com.exceptioncoder.llm.domain.service;

import java.util.Map;

/**
 * 工具执行服务 -- 提供工具直接调用能力，供 API 层调试和测试。
 *
 * <p>Infrastructure 层实现，委托给 ToolExecutor 完成反射调用。
 */
public interface ToolExecutionService {

    /**
     * 执行指定工具。
     *
     * @param toolId 工具 ID
     * @param params 输入参数
     * @return 执行结果（JSON 字符串）
     */
    String execute(String toolId, Map<String, Object> params);
}
