package com.exceptioncoder.llm.domain.registry;

import com.exceptioncoder.llm.domain.model.ToolDefinition;
import com.exceptioncoder.llm.domain.model.ToolType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 工具注册中心
 * 负责工具的注册、查询、注销
 */
public interface ToolRegistry {

    /**
     * 注册一个工具
     */
    void register(ToolDefinition definition, Object implementation);

    /**
     * 注销一个工具
     */
    void unregister(String toolId);

    /**
     * 获取工具实现
     */
    Optional<Object> getImplementation(String toolId);

    /**
     * 获取工具定义
     */
    Optional<ToolDefinition> getDefinition(String toolId);

    /**
     * 获取所有工具定义
     */
    List<ToolDefinition> getAllTools();

    /**
     * 按类型查询工具
     */
    List<ToolDefinition> getToolsByType(ToolType type);

    /**
     * 按标签查询工具
     */
    List<ToolDefinition> getToolsByTags(Set<String> tags);

    /**
     * 检查工具是否存在
     */
    boolean contains(String toolId);

    /**
     * 获取工具数量
     */
    int size();
}
