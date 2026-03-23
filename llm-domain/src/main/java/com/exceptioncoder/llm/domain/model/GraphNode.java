package com.exceptioncoder.llm.domain.model;

import java.util.Map;

/**
 * Graph 节点定义
 */
public record GraphNode(
        String id,
        NodeType type,
        String name,
        Map<String, Object> config
) {
    public GraphNode {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Node id 不能为空");
        }
        if (type == null) {
            throw new IllegalArgumentException("Node type 不能为空");
        }
        if (config == null) config = Map.of();
    }

    /**
     * 获取配置中的字符串值
     */
    public String getString(String key) {
        Object value = config.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取配置中的整数值
     */
    public Integer getInt(String key) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * 获取配置中的布尔值
     */
    public Boolean getBool(String key) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }
}
