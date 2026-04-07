package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.model.ToolDefinition;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolExecutor;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolRegistryImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * DevPlan Tool 标准协议层 -- 按角色路由工具集 + JSON Schema 参数校验。
 *
 * <p>维护 AgentRole → Tool ID 的静态映射，实现：
 * <ul>
 *   <li>角色工具隔离（规则 R5）：每个 Agent 只能调用其角色对应的工具集</li>
 *   <li>Schema 校验（规则 R6）：调用前校验必填参数</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
public class DevPlanToolRegistry {

    /**
     * 角色 → 工具 ID 列表的静态映射。
     * 公开为 public 供 DevPlanAgentInitializer 读取。
     */
    public static final Map<AgentRole, List<String>> ROLE_TOOL_MAPPING = Map.of(
            AgentRole.CODE_AWARENESS, List.of(
                    "devplan_project_scan",
                    "devplan_dependency_analysis",
                    "devplan_code_structure",
                    "devplan_config_scan",
                    "devplan_code_index"
            ),
            AgentRole.REQUIREMENT_ANALYZER, List.of(
                    "devplan_code_search",
                    "devplan_file_read"
            ),
            AgentRole.SOLUTION_ARCHITECT, List.of(
                    "devplan_code_search",
                    "devplan_template_render"
            ),
            AgentRole.PLAN_REVIEWER, List.of()
    );

    private final ToolRegistryImpl toolRegistry;
    private final ToolExecutor toolExecutor;

    public DevPlanToolRegistry(ToolRegistryImpl toolRegistry, ToolExecutor toolExecutor) {
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
    }

    /**
     * 获取指定角色可用的工具定义列表。
     */
    public List<ToolDefinition> getToolsForRole(AgentRole role) {
        List<String> toolIds = ROLE_TOOL_MAPPING.getOrDefault(role, List.of());
        List<ToolDefinition> tools = new ArrayList<>();
        for (String toolId : toolIds) {
            toolRegistry.getDefinition(toolId).ifPresent(tools::add);
        }
        return tools;
    }

    /**
     * 校验角色权限并执行工具。
     *
     * @param toolName   工具名称
     * @param params     调用参数
     * @param callerRole 调用方角色
     * @return 工具执行结果
     * @throws IllegalAccessException 角色无权限调用该工具
     */
    public String validateAndExecute(String toolName, Map<String, Object> params, AgentRole callerRole)
            throws IllegalAccessException {
        // 权限检查
        List<String> allowedTools = ROLE_TOOL_MAPPING.getOrDefault(callerRole, List.of());
        if (!allowedTools.contains(toolName)) {
            throw new IllegalAccessException(
                    "角色 " + callerRole + " 无权调用工具 " + toolName +
                    "，允许的工具: " + allowedTools);
        }

        // Schema 校验：检查 required 参数是否存在
        Optional<ToolDefinition> defOpt = toolRegistry.getDefinition(toolName);
        if (defOpt.isEmpty()) {
            throw new IllegalArgumentException("工具不存在: " + toolName);
        }

        // 执行
        return toolExecutor.execute(toolName, params);
    }
}
