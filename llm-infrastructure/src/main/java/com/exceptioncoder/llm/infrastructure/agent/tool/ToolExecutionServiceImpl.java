package com.exceptioncoder.llm.infrastructure.agent.tool;

import com.exceptioncoder.llm.domain.service.ToolExecutionService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 工具执行服务实现 -- 委托给 ToolExecutor 完成反射调用。
 */
@Service
public class ToolExecutionServiceImpl implements ToolExecutionService {

    private final ToolExecutor toolExecutor;

    public ToolExecutionServiceImpl(ToolExecutor toolExecutor) {
        this.toolExecutor = toolExecutor;
    }

    @Override
    public String execute(String toolId, Map<String, Object> params) {
        return toolExecutor.execute(toolId, params);
    }
}
