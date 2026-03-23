package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.domain.executor.GraphExecutor;
import com.exceptioncoder.llm.domain.model.GraphDefinition;
import com.exceptioncoder.llm.domain.model.GraphExecutionResult;
import com.exceptioncoder.llm.domain.registry.ToolRegistry;
import com.exceptioncoder.llm.domain.repository.GraphDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Graph 编排用例
 */
@Slf4j
@Service
public class GraphOrchestrationUseCase {

    private final GraphExecutor graphExecutor;
    private final GraphDefinitionRepository graphRepository;
    private final ToolRegistry toolRegistry;

    public GraphOrchestrationUseCase(
            GraphExecutor graphExecutor,
            GraphDefinitionRepository graphRepository,
            ToolRegistry toolRegistry
    ) {
        this.graphExecutor = graphExecutor;
        this.graphRepository = graphRepository;
        this.toolRegistry = toolRegistry;
    }

    /**
     * 执行 Graph
     */
    public GraphExecutionResult execute(String graphId, Map<String, Object> input) {
        var request = GraphExecutor.GraphExecutionRequest.builder()
                .executionId(UUID.randomUUID().toString())
                .graphId(graphId)
                .input(input != null ? input : Map.of())
                .build();
        log.info("执行 Graph: graphId={}", graphId);
        return graphExecutor.execute(request);
    }

    /**
     * 保存 Graph 定义
     */
    public GraphDefinition saveGraph(GraphDefinition graph) {
        graphExecutor.validateGraph(graph);
        return graphRepository.save(graph);
    }

    /**
     * 获取所有 Graph
     */
    public List<GraphDefinition> getAllGraphs() {
        return graphRepository.findAll();
    }

    /**
     * 获取单个 Graph
     */
    public GraphDefinition getGraph(String graphId) {
        return graphRepository.findById(graphId)
                .orElseThrow(() -> new IllegalArgumentException("Graph 不存在: " + graphId));
    }

    /**
     * 删除 Graph
     */
    public void deleteGraph(String graphId) {
        graphRepository.deleteById(graphId);
    }
}
