package com.exceptioncoder.llm.infrastructure.config;

import com.exceptioncoder.llm.domain.executor.AgentExecutor;
import com.exceptioncoder.llm.domain.executor.GraphExecutor;
import com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository;
import com.exceptioncoder.llm.domain.repository.GraphDefinitionRepository;
import com.exceptioncoder.llm.domain.registry.ToolRegistry;
import com.exceptioncoder.llm.infrastructure.agent.executor.AlibabaAgentExecutor;
import com.exceptioncoder.llm.infrastructure.agent.graph.GraphExecutionEngine;
import com.exceptioncoder.llm.infrastructure.agent.graph.GraphExecutorImpl;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolExecutor;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolRegistryImpl;
import com.exceptioncoder.llm.infrastructure.provider.LLMProviderRouter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 运行时 Spring 配置
 */
@Configuration
public class AgentConfiguration {

    @Bean
    public AgentExecutor agentExecutor(
            AgentDefinitionRepository agentRepository,
            ToolRegistryImpl toolRegistry,
            ToolExecutor toolExecutor,
            LLMProviderRouter providerRouter
    ) {
        return new AlibabaAgentExecutor(agentRepository, toolRegistry, toolExecutor, providerRouter);
    }

    @Bean
    public GraphExecutionEngine graphExecutionEngine(
            AgentExecutor agentExecutor,
            ToolExecutor toolExecutor,
            LLMConfiguration llmConfig
    ) {
        return new GraphExecutionEngine(agentExecutor, toolExecutor, llmConfig);
    }

    @Bean
    public GraphExecutor graphExecutor(
            GraphDefinitionRepository graphRepository,
            GraphExecutionEngine graphExecutionEngine
    ) {
        return new GraphExecutorImpl(graphRepository, graphExecutionEngine);
    }
}
