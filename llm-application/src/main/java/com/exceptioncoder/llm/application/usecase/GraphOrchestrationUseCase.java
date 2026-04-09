package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.domain.executor.GraphExecutor;
import com.exceptioncoder.llm.domain.model.*;
import com.exceptioncoder.llm.domain.registry.ToolRegistry;
import com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository;
import com.exceptioncoder.llm.domain.repository.GraphDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Graph 编排用例
 */
@Slf4j
@Service
public class GraphOrchestrationUseCase {

    private final GraphExecutor graphExecutor;
    private final GraphDefinitionRepository graphRepository;
    private final AgentDefinitionRepository agentRepository;
    private final ToolRegistry toolRegistry;

    public GraphOrchestrationUseCase(
            GraphExecutor graphExecutor,
            GraphDefinitionRepository graphRepository,
            AgentDefinitionRepository agentRepository,
            ToolRegistry toolRegistry
    ) {
        this.graphExecutor = graphExecutor;
        this.graphRepository = graphRepository;
        this.agentRepository = agentRepository;
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

    /**
     * 获取 Graph 下关联的 Agent 列表（从节点 config 中提取 agentId）
     */
    public List<AgentDefinition> getGraphAgents(String graphId) {
        GraphDefinition graph = getGraph(graphId);
        return graph.nodes().stream()
                .map(node -> node.getString("agentId"))
                .filter(Objects::nonNull)
                .distinct()
                .map(agentRepository::findById)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * 获取 Graph 的调用链（按拓扑顺序展示节点间的流转关系）
     */
    public List<CallChainStep> getGraphCallChain(String graphId) {
        GraphDefinition graph = getGraph(graphId);
        List<CallChainStep> chain = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();

        traverseChain(graph, graph.entryNodeId(), visited, chain);
        return chain;
    }

    private void traverseChain(GraphDefinition graph, String nodeId,
                                Set<String> visited, List<CallChainStep> chain) {
        if (nodeId == null || visited.contains(nodeId)) {
            return;
        }
        visited.add(nodeId);

        GraphNode node = graph.findNode(nodeId);
        if (node == null) {
            return;
        }

        // 收集当前节点的出边目标
        List<String> nextNodeIds = graph.getOutgoingEdges(nodeId).stream()
                .map(GraphEdge::to)
                .toList();

        // 解析节点关联的 agentId
        String agentId = node.getString("agentId");
        String agentName = null;
        if (agentId != null) {
            agentName = agentRepository.findById(agentId)
                    .map(AgentDefinition::name)
                    .orElse(null);
        }

        chain.add(new CallChainStep(
                chain.size() + 1,
                node.id(),
                node.name(),
                node.type(),
                agentId,
                agentName,
                nextNodeIds
        ));

        // 递归遍历后续节点
        for (String nextId : nextNodeIds) {
            traverseChain(graph, nextId, visited, chain);
        }
    }

    /**
     * 调用链步骤 VO
     */
    public record CallChainStep(
            int order,
            String nodeId,
            String nodeName,
            NodeType nodeType,
            String agentId,
            String agentName,
            List<String> nextNodeIds
    ) {}
}
