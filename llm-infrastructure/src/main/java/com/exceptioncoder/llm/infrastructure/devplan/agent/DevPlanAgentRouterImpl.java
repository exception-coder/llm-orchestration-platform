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
 * Agent 路由实现 — 按角色分发到对应 Agent 执行
 */
@Slf4j
@Service
public class DevPlanAgentRouterImpl implements DevPlanAgentRouter {

    private final AgentExecutor agentExecutor;
    private final DevPlanMemoryManager memoryManager;
    private final DevPlanAgentConfig agentConfig;

    public DevPlanAgentRouterImpl(AgentExecutor agentExecutor,
                                   DevPlanMemoryManager memoryManager,
                                   DevPlanAgentConfig agentConfig) {
        this.agentExecutor = agentExecutor;
        this.memoryManager = memoryManager;
        this.agentConfig = agentConfig;
    }

    @Override
    public AgentOutput route(AgentRole role, DevPlanState state) {
        log.info("路由到 Agent，role={}，taskId={}", role, state.taskId());
        long start = System.currentTimeMillis();

        // 加载记忆上下文
        Map<String, Object> memoryContext = memoryManager.loadContext(
                state.taskId(),
                state.requirement()
        );

        // 构建 Agent 执行上下文
        Map<String, Object> context = new HashMap<>(memoryContext);
        context.put("state", state);
        context.put("role", role.name());
        context.put("systemPrompt", agentConfig.getSystemPrompt(role));

        // 构建用户输入
        String userInput = buildUserInput(role, state);

        // 执行 Agent
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

        return AgentOutput.builder()
                .role(role)
                .rawOutput(result.finalOutput())
                .structuredData(parseStructuredOutput(role, result))
                .tokenUsage(0) // TODO: 从 result 中提取实际 token 用量
                .elapsedMs(elapsed)
                .build();
    }

    private String buildUserInput(AgentRole role, DevPlanState state) {
        return switch (role) {
            case CODE_AWARENESS -> "请分析项目代码结构：" + state.projectPath();
            case REQUIREMENT_ANALYZER -> "请分析以下需求的影响范围：\n" + state.requirement();
            case SOLUTION_ARCHITECT -> buildDesignInput(state);
            case PLAN_REVIEWER -> "请审查以下设计文档：\n" +
                    (state.document() != null ? state.document().fullDocument() : "");
        };
    }

    private String buildDesignInput(DevPlanState state) {
        StringBuilder sb = new StringBuilder("请基于以下分析结果生成设计文档：\n");
        sb.append("需求：").append(state.requirement()).append("\n");
        if (state.impact() != null) {
            sb.append("影响范围：").append(state.impact().affectedClasses()).append("\n");
        }
        if (!state.reviewIssues().isEmpty()) {
            sb.append("\n【修正要求 — 仅关注以下失败项】：\n");
            state.reviewIssues().forEach(issue -> sb.append("- ").append(issue).append("\n"));
        }
        return sb.toString();
    }

    private Map<String, Object> parseStructuredOutput(AgentRole role, AgentExecutionResult result) {
        // TODO: 解析 Agent 输出为结构化数据
        // 当前返回 raw output，后续实现 JSON 解析
        Map<String, Object> data = new HashMap<>();
        data.put("rawOutput", result.finalOutput());
        return data;
    }
}
