package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.domain.model.AgentDefinition;
import com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository;
import com.exceptioncoder.llm.infrastructure.entity.agent.AgentDefinitionEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Agent 定义仓储实现
 */
@Slf4j
@Repository
public class AgentDefinitionRepositoryImpl implements AgentDefinitionRepository {

    private final AgentDefinitionJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public AgentDefinitionRepositoryImpl(AgentDefinitionJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentDefinition save(AgentDefinition agent) {
        var existing = jpaRepository.findById(agent.id());
        AgentDefinitionEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setName(agent.name());
            entity.setDescription(agent.description());
            entity.setSystemPrompt(agent.systemPrompt());
            entity.setToolIds(toJson(agent.toolIds()));
            entity.setLlmProvider(agent.llmProvider());
            entity.setLlmModel(agent.llmModel());
            entity.setMaxIterations(agent.maxIterations());
            entity.setTimeoutSeconds(agent.timeoutSeconds());
            entity.setEnabled(agent.enabled());
        } else {
            entity = AgentDefinitionEntity.builder()
                    .id(agent.id())
                    .name(agent.name())
                    .description(agent.description())
                    .systemPrompt(agent.systemPrompt())
                    .toolIds(toJson(agent.toolIds()))
                    .llmProvider(agent.llmProvider())
                    .llmModel(agent.llmModel())
                    .maxIterations(agent.maxIterations())
                    .timeoutSeconds(agent.timeoutSeconds())
                    .enabled(agent.enabled())
                    .build();
        }
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<AgentDefinition> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<AgentDefinition> findAllEnabled() {
        return jpaRepository.findByEnabledTrue().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgentDefinition> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsById(id);
    }

    private AgentDefinition toDomain(AgentDefinitionEntity entity) {
        return AgentDefinition.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .systemPrompt(entity.getSystemPrompt())
                .toolIds(fromJson(entity.getToolIds()))
                .llmProvider(entity.getLlmProvider())
                .llmModel(entity.getLlmModel())
                .maxIterations(entity.getMaxIterations())
                .timeoutSeconds(entity.getTimeoutSeconds())
                .enabled(entity.isEnabled())
                .build();
    }

    private String toJson(Object obj) {
        try {
            return obj != null ? objectMapper.writeValueAsString(obj) : null;
        } catch (Exception e) {
            log.error("序列化失败", e);
            return null;
        }
    }

    private List<String> fromJson(String json) {
        try {
            return json != null ? objectMapper.readValue(json, new TypeReference<List<String>>() {}) : Collections.emptyList();
        } catch (Exception e) {
            log.error("反序列化失败: {}", json, e);
            return Collections.emptyList();
        }
    }
}
