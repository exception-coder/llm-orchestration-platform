package com.exceptioncoder.llm.infrastructure.devplan.repository;

import com.exceptioncoder.llm.domain.devplan.model.ArchTopology;
import com.exceptioncoder.llm.domain.devplan.repository.ProjectArchTopologyRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 架构拓扑仓储 — 内存实现（一期先用内存，后续切换 JPA）
 */
@Repository
public class InMemoryProjectArchTopologyRepository implements ProjectArchTopologyRepository {

    private final Map<String, ArchTopology> store = new ConcurrentHashMap<>();

    @Override
    public Optional<ArchTopology> findByProjectPath(String projectPath) {
        return Optional.ofNullable(store.get(projectPath));
    }

    @Override
    public void save(ArchTopology topology) {
        store.put(topology.projectPath(), topology);
    }
}
