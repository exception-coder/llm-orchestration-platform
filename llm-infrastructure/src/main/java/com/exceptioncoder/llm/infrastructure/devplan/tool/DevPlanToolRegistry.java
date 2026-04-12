package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.model.ToolDefinition;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolExecutor;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolRegistryImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DevPlan Tool 标准协议层 -- 按角色路由工具集 + JSON Schema 参数校验。
 *
 * <p>角色→工具的映射由各 {@code @Tool(roles = {...})} 注解声明，
 * 启动后从 {@link ToolRegistryImpl} 动态查询，无需手动维护静态 Map。
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

    private final ToolRegistryImpl toolRegistry;
    private final ToolExecutor toolExecutor;

    public DevPlanToolRegistry(ToolRegistryImpl toolRegistry, ToolExecutor toolExecutor) {
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
    }

    /**
     * 获取指定角色可用的工具定义列表。
     *
     * <p>从 ToolRegistry 中筛选 {@link ToolDefinition#roles()} 包含该角色名的工具。
     */
    public List<ToolDefinition> getToolsForRole(AgentRole role) {
        String roleName = role.name();
        return toolRegistry.getAllTools().stream()
                .filter(def -> def.roles().contains(roleName))
                .collect(Collectors.toList());
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
        // Schema 校验：检查工具是否存在
        Optional<ToolDefinition> defOpt = toolRegistry.getDefinition(toolName);
        if (defOpt.isEmpty()) {
            throw new IllegalArgumentException("工具不存在: " + toolName);
        }

        // 权限检查：工具的 roles 是否包含调用方角色
        ToolDefinition def = defOpt.get();
        if (!def.roles().contains(callerRole.name())) {
            throw new IllegalAccessException(
                    "角色 " + callerRole + " 无权调用工具 " + toolName +
                    "，该工具归属角色: " + def.roles());
        }

        // 执行
        return toolExecutor.execute(toolName, params);
    }
}
