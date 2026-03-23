package com.exceptioncoder.llm.domain.model;

/**
 * Graph 边定义（节点之间的连接）
 */
public record GraphEdge(
        String from,
        String to,
        String condition
) {
    public GraphEdge {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("Edge from 不能为空");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Edge to 不能为空");
        }
    }

    /**
     * 是否为无条件边
     */
    public boolean isUnconditional() {
        return condition == null || condition.isBlank();
    }
}
