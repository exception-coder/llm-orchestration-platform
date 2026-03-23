package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.domain.executor.AgentExecutor;
import com.exceptioncoder.llm.domain.model.AgentDefinition;
import com.exceptioncoder.llm.domain.model.AgentExecutionResult;
import com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository;
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

    public AgentExecutionUseCase(AgentExecutor agentExecutor, AgentDefinitionRepository agentRepository) {
        this.agentExecutor = agentExecutor;
        this.agentRepository = agentRepository;
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
        log.info("执行 Agent: agentId={}, input={}", agentId, userInput);
        return agentExecutor.execute(request);
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
}
