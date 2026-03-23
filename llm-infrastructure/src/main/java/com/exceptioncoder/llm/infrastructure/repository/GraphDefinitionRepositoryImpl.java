package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.domain.model.GraphDefinition;
import com.exceptioncoder.llm.domain.model.GraphEdge;
import com.exceptioncoder.llm.domain.model.GraphNode;
import com.exceptioncoder.llm.domain.repository.GraphDefinitionRepository;
import com.exceptioncoder.llm.infrastructure.entity.agent.GraphDefinitionEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Graph 定义仓储实现
 */
@Slf4j
@Repository
public class GraphDefinitionRepositoryImpl implements GraphDefinitionRepository {

    private final GraphDefinitionJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public GraphDefinitionRepositoryImpl(GraphDefinitionJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public GraphDefinition save(GraphDefinition graph) {
        var existing = jpaRepository.findById(graph.id());
        GraphDefinitionEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setName(graph.name());
            entity.setDescription(graph.description());
            entity.setNodes(toJson(graph.nodes()));
            entity.setEdges(toJson(graph.edges()));
            entity.setEntryNodeId(graph.entryNodeId());
        } else {
            entity = GraphDefinitionEntity.builder()
                    .id(graph.id())
                    .name(graph.name())
                    .description(graph.description())
                    .nodes(toJson(graph.nodes()))
                    .edges(toJson(graph.edges()))
                    .entryNodeId(graph.entryNodeId())
                    .enabled(true)
                    .build();
        }
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<GraphDefinition> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<GraphDefinition> findAllEnabled() {
        return jpaRepository.findByEnabledTrue().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<GraphDefinition> findAll() {
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

    private GraphDefinition toDomain(GraphDefinitionEntity entity) {
        List<GraphNode> nodes = fromJson(entity.getNodes(), new TypeReference<List<GraphNode>>() {});
        List<GraphEdge> edges = fromJson(entity.getEdges(), new TypeReference<List<GraphEdge>>() {});
        return new GraphDefinition(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                nodes,
                edges,
                entity.getEntryNodeId()
        );
    }

    private String toJson(Object obj) {
        try {
            return obj != null ? objectMapper.writeValueAsString(obj) : null;
        } catch (Exception e) {
            log.error("序列化失败", e);
            return null;
        }
    }

    private <T> T fromJson(String json, TypeReference<T> type) {
        try {
            return json != null ? objectMapper.readValue(json, type) : null;
        } catch (Exception e) {
            log.error("反序列化失败: {}", json, e);
            return null;
        }
    }
}
