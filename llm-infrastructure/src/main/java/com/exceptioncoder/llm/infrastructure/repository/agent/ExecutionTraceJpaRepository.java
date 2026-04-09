package com.exceptioncoder.llm.infrastructure.repository.agent;

import com.exceptioncoder.llm.infrastructure.entity.agent.ExecutionTraceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionTraceJpaRepository extends JpaRepository<ExecutionTraceEntity, String> {

    List<ExecutionTraceEntity> findByAgentIdOrderByCreatedAtDesc(String agentId);

    List<ExecutionTraceEntity> findTop50ByOrderByCreatedAtDesc();

    List<ExecutionTraceEntity> findTop20ByAgentIdOrderByCreatedAtDesc(String agentId);
}
