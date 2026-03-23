package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.infrastructure.entity.agent.GraphDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GraphDefinitionJpaRepository extends JpaRepository<GraphDefinitionEntity, String> {
    List<GraphDefinitionEntity> findByEnabledTrue();
}
