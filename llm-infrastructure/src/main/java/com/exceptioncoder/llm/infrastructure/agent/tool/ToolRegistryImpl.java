package com.exceptioncoder.llm.infrastructure.agent.tool;

import com.exceptioncoder.llm.domain.model.ToolDefinition;
import com.exceptioncoder.llm.domain.model.ToolType;
import com.exceptioncoder.llm.domain.registry.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 工具注册中心实现
 */
@Slf4j
@Component
public class ToolRegistryImpl implements ToolRegistry {

    private final Map<String, ToolDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, Object> implementations = new ConcurrentHashMap<>();

    @Override
    public void register(ToolDefinition definition, Object implementation) {
        if (definitions.containsKey(definition.id())) {
            log.warn("工具 {} 已存在，将被覆盖", definition.id());
        }
        definitions.put(definition.id(), definition);
        implementations.put(definition.id(), implementation);
        log.info("注册工具: id={}, name={}, type={}",
                definition.id(), definition.name(), definition.type());
    }

    @Override
    public void unregister(String toolId) {
        definitions.remove(toolId);
        implementations.remove(toolId);
        log.info("注销工具: {}", toolId);
    }

    @Override
    public Optional<Object> getImplementation(String toolId) {
        return Optional.ofNullable(implementations.get(toolId));
    }

    @Override
    public Optional<ToolDefinition> getDefinition(String toolId) {
        return Optional.ofNullable(definitions.get(toolId));
    }

    @Override
    public List<ToolDefinition> getAllTools() {
        return new ArrayList<>(definitions.values());
    }

    @Override
    public List<ToolDefinition> getToolsByType(ToolType type) {
        return definitions.values().stream()
                .filter(t -> t.type() == type)
                .collect(Collectors.toList());
    }

    @Override
    public List<ToolDefinition> getToolsByTags(Set<String> tags) {
        return definitions.values().stream()
                .filter(t -> {
                    // TODO: ToolDefinition 中增加 tags 字段
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean contains(String toolId) {
        return definitions.containsKey(toolId);
    }

    @Override
    public int size() {
        return definitions.size();
    }
}
