package com.exceptioncoder.llm.infrastructure.devplan.agent;

import com.exceptioncoder.llm.domain.devplan.model.AgentOutput;
import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.service.DevPlanAgentRouter;
import com.exceptioncoder.llm.domain.devplan.service.DevPlanMemoryManager;
import com.exceptioncoder.llm.domain.executor.AgentExecutor;
import com.exceptioncoder.llm.domain.executor.AgentExecutor.AgentExecutionRequest;
import com.exceptioncoder.llm.domain.model.AgentExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 开发计划 Agent 路由器实现，按角色将任务分发到对应的 Agent 执行。
 *
 * <p>属于 Infrastructure层 devplan/agent 模块，实现了 Domain 层定义的
 * {@link DevPlanAgentRouter} 接口。本类是多 Agent 协作流程中的关键枢纽，
 * 根据 {@link AgentRole} 决定调用哪个 Agent、使用什么 System Prompt、
 * 传入什么上下文信息。
 *
 * <p><b>设计思路：</b>
 * <ul>
 *   <li>Agent 配置（ID、Prompt）统一由 {@link DevPlanAgentConfig} 管理，路由器不硬编码</li>
 *   <li>记忆上下文通过 {@link DevPlanMemoryManager} 加载，实现"带记忆的 Agent 调用"</li>
 *   <li>用户输入根据角色动态构建，不同角色接收不同格式的输入信息</li>
 * </ul>
 *
 * <p><b>协作关系：</b>
 * <ul>
 *   <li>实现 {@link DevPlanAgentRouter}（Domain 层接口）</li>
 *   <li>依赖 {@link AgentExecutor} 执行实际的 Agent 调用</li>
 *   <li>依赖 {@link DevPlanMemoryManager} 加载三级记忆上下文</li>
 *   <li>依赖 {@link DevPlanAgentConfig} 获取角色对应的 Agent ID 和 System Prompt</li>
 *   <li>被 Domain 层的编排服务（DevPlanOrchestrator）通过接口调用</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Service
public class DevPlanAgentRouterImpl implements DevPlanAgentRouter {

    /** Agent 执行器，负责实际调用 LLM Agent */
    private final AgentExecutor agentExecutor;
    /** 记忆管理器，提供短期/长期/结构化三级记忆上下文 */
    private final DevPlanMemoryManager memoryManager;
    /** Agent 配置，维护各角色的 Agent ID 和 System Prompt 映射 */
    private final DevPlanAgentConfig agentConfig;

    /**
     * 构造 Agent 路由器，注入所需的协作组件。
     *
     * @param agentExecutor Agent 执行器
     * @param memoryManager 记忆管理器
     * @param agentConfig   Agent 配置
     */
    public DevPlanAgentRouterImpl(AgentExecutor agentExecutor,
                                   DevPlanMemoryManager memoryManager,
                                   DevPlanAgentConfig agentConfig) {
        this.agentExecutor = agentExecutor;
        this.memoryManager = memoryManager;
        this.agentConfig = agentConfig;
    }

    /**
     * 根据角色将任务路由到对应的 Agent 并执行。
     *
     * <p>执行流程：
     * <ol>
     *   <li>从记忆管理器加载上下文（短期记忆 + 长期记忆 + 结构化拓扑）</li>
     *   <li>合并记忆上下文与当前任务状态，构建完整的执行上下文</li>
     *   <li>根据角色构建差异化的用户输入文本</li>
     *   <li>构建 AgentExecutionRequest 并提交给 AgentExecutor 执行</li>
     *   <li>封装执行结果为 AgentOutput 返回</li>
     * </ol>
     *
     * @param role  Agent 角色，决定调用哪个 Agent 以及使用什么 Prompt
     * @param state 当前开发计划的流程状态，包含项目路径、需求、已有分析结果等
     * @return Agent 执行输出，包含原始输出、结构化数据、耗时等信息
     */
    @Override
    public AgentOutput route(AgentRole role, DevPlanState state) {
        log.info("路由到 Agent，role={}，taskId={}", role, state.taskId());
        long start = System.currentTimeMillis();

        // 第一步：加载三级记忆上下文（短期/长期/结构化）
        Map<String, Object> memoryContext = memoryManager.loadContext(
                state.taskId(),
                state.requirement()
        );

        // 第二步：合并记忆上下文与当前任务状态，构建完整执行上下文
        Map<String, Object> context = new HashMap<>(memoryContext);
        context.put("state", state);
        context.put("role", role.name());
        context.put("systemPrompt", agentConfig.getSystemPrompt(role));

        // 第三步：根据角色构建差异化的用户输入
        String userInput = buildUserInput(role, state);

        // 第四步：构建请求并执行 Agent
        AgentExecutionRequest request = AgentExecutionRequest.builder()
                .executionId(UUID.randomUUID().toString())
                .agentId(agentConfig.getAgentId(role))
                .userInput(userInput)
                .context(context)
                .build();

        AgentExecutionResult result = agentExecutor.execute(request);

        long elapsed = System.currentTimeMillis() - start;
        log.info("Agent 执行完成，role={}，iterations={}，elapsedMs={}",
                role, result.iterations(), elapsed);

        // 第五步：封装输出结果
        return AgentOutput.builder()
                .role(role)
                .rawOutput(result.finalOutput())
                .structuredData(parseStructuredOutput(role, result))
                .tokenUsage(0) // TODO: 从 result 中提取实际 token 用量
                .elapsedMs(elapsed)
                .build();
    }

    /**
     * 根据 Agent 角色构建差异化的用户输入文本。
     *
     * <p>不同角色关注不同维度的信息：
     * <ul>
     *   <li>CODE_AWARENESS — 关注项目路径，用于代码结构扫描</li>
     *   <li>REQUIREMENT_ANALYZER — 关注需求文本，用于影响范围分析</li>
     *   <li>SOLUTION_ARCHITECT — 综合需求 + 影响分析 + 历史审查问题，用于方案设计</li>
     *   <li>PLAN_REVIEWER — 关注设计文档全文，用于质量评审</li>
     * </ul>
     *
     * @param role  Agent 角色
     * @param state 当前流程状态
     * @return 构建好的用户输入文本
     */
    private String buildUserInput(AgentRole role, DevPlanState state) {
        return switch (role) {
            case CODE_AWARENESS -> "请分析项目代码结构：" + state.projectPath();
            case REQUIREMENT_ANALYZER -> "请分析以下需求的影响范围：\n" + state.requirement();
            case SOLUTION_ARCHITECT -> buildDesignInput(state);
            case PLAN_REVIEWER -> "请审查以下设计文档：\n" +
                    (state.document() != null ? state.document().fullDocument() : "");
        };
    }

    /**
     * 构建方案架构师（SOLUTION_ARCHITECT）的输入文本。
     *
     * <p>该输入包含三部分信息：需求描述、影响范围分析结果、
     * 以及前一轮审查中发现的问题（如果存在，用于驱动迭代修正）。
     *
     * @param state 当前流程状态
     * @return 包含完整上下文的设计输入文本
     */
    private String buildDesignInput(DevPlanState state) {
        StringBuilder sb = new StringBuilder("请基于以下分析结果生成设计文档：\n");
        sb.append("需求：").append(state.requirement()).append("\n");
        if (state.impact() != null) {
            sb.append("影响范围：").append(state.impact().affectedClasses()).append("\n");
        }
        // 如果存在审查问题，附加修正要求，驱动 Agent 迭代改进
        if (!state.reviewIssues().isEmpty()) {
            sb.append("\n【修正要求 — 仅关注以下失败项】：\n");
            state.reviewIssues().forEach(issue -> sb.append("- ").append(issue).append("\n"));
        }
        return sb.toString();
    }

    /**
     * 解析 Agent 执行结果为结构化数据。
     *
     * <p>当前为简化实现，直接将原始输出包装为 Map 返回；
     * 后续需根据不同角色的输出格式实现 JSON 解析，
     * 提取如 ProjectStructure、ImpactAnalysis、DesignDocument 等领域对象。
     *
     * @param role   Agent 角色，决定解析策略
     * @param result Agent 执行结果
     * @return 结构化数据 Map
     */
    private Map<String, Object> parseStructuredOutput(AgentRole role, AgentExecutionResult result) {
        // TODO: 按角色实现差异化的 JSON 解析逻辑
        Map<String, Object> data = new HashMap<>();
        data.put("rawOutput", result.finalOutput());
        return data;
    }
}
