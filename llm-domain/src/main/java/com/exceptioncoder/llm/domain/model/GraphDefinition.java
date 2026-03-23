package com.exceptioncoder.llm.domain.model;

import java.util.List;

/**
 * Graph 流程定义
 */
public record GraphDefinition(
        String id,
        String name,
        String description,
        List<GraphNode> nodes,
        List<GraphEdge> edges,
        String entryNodeId
) {
    public GraphDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Graph id 不能为空");
        }
        if (nodes == null) nodes = List.of();
        if (edges == null) edges = List.of();
    }

    /**
     * 根据节点 ID 查找节点
     */
    public GraphNode findNode(String nodeId) {
        return nodes.stream()
                .filter(n -> n.id().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取指定节点的所有出边
     */
    public List<GraphEdge> getOutgoingEdges(String nodeId) {
        return edges.stream()
                .filter(e -> e.from().equals(nodeId))
                .toList();
    }
}
