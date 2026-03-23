package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.infrastructure.entity.agent.AgentDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentDefinitionJpaRepository extends JpaRepository<AgentDefinitionEntity, String> {
    List<AgentDefinitionEntity> findByEnabledTrue();
}
