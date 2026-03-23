package com.exceptioncoder.llm.api.controller;

import com.exceptioncoder.llm.application.usecase.GraphOrchestrationUseCase;
import com.exceptioncoder.llm.domain.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Graph REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/graphs")
public class GraphController {

    private final GraphOrchestrationUseCase graphOrchestrationUseCase;

    public GraphController(GraphOrchestrationUseCase graphOrchestrationUseCase) {
        this.graphOrchestrationUseCase = graphOrchestrationUseCase;
    }

    /** 获取所有 Graph */
    @GetMapping
    public ResponseEntity<List<GraphDefinition>> listGraphs() {
        return ResponseEntity.ok(graphOrchestrationUseCase.getAllGraphs());
    }

    /** 获取单个 Graph */
    @GetMapping("/{graphId}")
    public ResponseEntity<GraphDefinition> getGraph(@PathVariable String graphId) {
        return ResponseEntity.ok(graphOrchestrationUseCase.getGraph(graphId));
    }

    /** 创建或更新 Graph 定义 */
    @PostMapping
    public ResponseEntity<GraphDefinition> saveGraph(@RequestBody GraphDefinition graph) {
        return ResponseEntity.ok(graphOrchestrationUseCase.saveGraph(graph));
    }

    /** 删除 Graph */
    @DeleteMapping("/{graphId}")
    public ResponseEntity<Void> deleteGraph(@PathVariable String graphId) {
        graphOrchestrationUseCase.deleteGraph(graphId);
        return ResponseEntity.noContent().build();
    }

    /** 执行 Graph */
    @PostMapping("/{graphId}/execute")
    public ResponseEntity<GraphExecutionResult> execute(
            @PathVariable String graphId,
            @RequestBody GraphExecuteRequest request
    ) {
        GraphExecutionResult result = graphOrchestrationUseCase.execute(
                graphId,
                request.input()
        );
        return ResponseEntity.ok(result);
    }

    public record GraphExecuteRequest(Map<String, Object> input) {}
}
