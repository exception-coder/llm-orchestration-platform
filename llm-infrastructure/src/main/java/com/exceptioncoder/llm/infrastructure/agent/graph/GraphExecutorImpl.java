package com.exceptioncoder.llm.infrastructure.agent.graph;

import com.exceptioncoder.llm.domain.executor.GraphExecutor;
import com.exceptioncoder.llm.domain.model.GraphDefinition;
import com.exceptioncoder.llm.domain.model.GraphExecutionResult;
import com.exceptioncoder.llm.domain.repository.GraphDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

/**
 * Graph 执行器实现
 */
@Slf4j
public class GraphExecutorImpl implements GraphExecutor {

    private final GraphDefinitionRepository graphRepository;
    private final GraphExecutionEngine engine;

    public GraphExecutorImpl(GraphDefinitionRepository graphRepository, GraphExecutionEngine engine) {
        this.graphRepository = graphRepository;
        this.engine = engine;
    }

    @Override
    public GraphExecutionResult execute(GraphExecutionRequest request) {
        GraphDefinition graph = loadGraph(request.graphId());
        String executionId = request.executionId() != null
                ? request.executionId()
                : UUID.randomUUID().toString();
        log.info("开始执行 Graph: graphId={}, executionId={}", request.graphId(), executionId);
        return engine.execute(graph, request.input(), executionId);
    }

    @Override
    public Flux<String> executeStream(GraphExecutionRequest request) {
        return Flux.defer(() -> {
            GraphExecutionResult result = execute(request);
            return Flux.just(result.finalOutput() != null ? result.finalOutput() : "");
        });
    }

    @Override
    public GraphDefinition loadGraph(String graphId) {
        return graphRepository.findById(graphId)
                .orElseThrow(() -> new IllegalArgumentException("Graph 不存在: " + graphId));
    }

    @Override
    public void validateGraph(GraphDefinition graph) {
        engine.validateGraph(graph);
    }
}
