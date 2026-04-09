package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.domain.executor.AgentExecutor;
import com.exceptioncoder.llm.domain.model.*;
import com.exceptioncoder.llm.domain.registry.ToolRegistry;
import com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository;
import com.exceptioncoder.llm.domain.repository.ExecutionTraceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 执行用例
 */
@Slf4j
@Service
public class AgentExecutionUseCase {

    private final AgentExecutor agentExecutor;
    private final AgentDefinitionRepository agentRepository;
    private final ToolRegistry toolRegistry;
    private final ExecutionTraceRepository traceRepository;

    public AgentExecutionUseCase(AgentExecutor agentExecutor,
                                 AgentDefinitionRepository agentRepository,
                                 ToolRegistry toolRegistry,
                                 ExecutionTraceRepository traceRepository) {
        this.agentExecutor = agentExecutor;
        this.agentRepository = agentRepository;
        this.toolRegistry = toolRegistry;
        this.traceRepository = traceRepository;
    }

    /**
     * 执行 Agent
     */
    public AgentExecutionResult execute(String agentId, String userInput, Map<String, Object> context) {
        var request = AgentExecutor.AgentExecutionRequest.builder()
                .executionId(UUID.randomUUID().toString())
                .agentId(agentId)
                .userInput(userInput)
                .context(context != null ? context : Map.of())
                .build();
        log.info("执行 Agent: agentId={}, traceId={}, input={}", agentId, request.executionId(), userInput);

        AgentExecutionResult result = agentExecutor.execute(request);

        // 持久化执行流水
        try {
            String agentName = agentRepository.findById(agentId)
                    .map(AgentDefinition::name).orElse(agentId);
            ExecutionTrace trace = ExecutionTrace.fromResult(result, agentName, userInput);
            traceRepository.save(trace);
            log.info("执行流水已记录: traceId={}, status={}, steps={}",
                    trace.traceId(), trace.status(), trace.steps().size());
        } catch (Exception e) {
            log.warn("执行流水持久化失败（不影响主流程）: traceId={}", result.executionId(), e);
        }

        return result;
    }

    /**
     * 按流水号查询执行流水明细。
     */
    public ExecutionTrace getTrace(String traceId) {
        return traceRepository.findByTraceId(traceId)
                .orElseThrow(() -> new IllegalArgumentException("流水号不存在: " + traceId));
    }

    /**
     * 查询指定 Agent 的最近执行记录。
     */
    public List<ExecutionTrace> getAgentTraces(String agentId, int limit) {
        return traceRepository.findByAgentId(agentId, limit);
    }

    /**
     * 查询最近执行记录。
     */
    public List<ExecutionTrace> getRecentTraces(int limit) {
        return traceRepository.findRecent(limit);
    }

    /**
     * 保存 Agent 定义
     */
    public AgentDefinition saveAgent(AgentDefinition agent) {
        return agentRepository.save(agent);
    }

    /**
     * 获取所有 Agent
     */
    public List<AgentDefinition> getAllAgents() {
        return agentRepository.findAll();
    }

    /**
     * 获取单个 Agent
     */
    public AgentDefinition getAgent(String agentId) {
        return agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent 不存在: " + agentId));
    }

    /**
     * 删除 Agent
     */
    public void deleteAgent(String agentId) {
        agentRepository.deleteById(agentId);
    }

    /**
     * 获取指定 Agent 关联的 Tool 详情列表
     */
    public List<ToolDefinition> getAgentTools(String agentId) {
        AgentDefinition agent = getAgent(agentId);
        if (agent.toolIds() == null || agent.toolIds().isEmpty()) {
            return List.of();
        }
        return agent.toolIds().stream()
                .map(toolRegistry::getDefinition)
                .flatMap(java.util.Optional::stream)
                .toList();
    }
}
