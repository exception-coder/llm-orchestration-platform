package com.exceptioncoder.llm.domain.devplan.repository;

import com.exceptioncoder.llm.domain.devplan.model.ArchTopology;

import java.util.Optional;

/**
 * 架构拓扑仓储接口（结构化记忆持久化）
 */
public interface ProjectArchTopologyRepository {

    Optional<ArchTopology> findByProjectPath(String projectPath);

    void save(ArchTopology topology);
}
