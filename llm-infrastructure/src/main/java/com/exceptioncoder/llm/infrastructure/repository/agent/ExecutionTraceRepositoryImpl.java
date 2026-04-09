package com.exceptioncoder.llm.infrastructure.repository.agent;

import com.exceptioncoder.llm.domain.model.ExecutionStep;
import com.exceptioncoder.llm.domain.model.ExecutionTrace;
import com.exceptioncoder.llm.domain.repository.ExecutionTraceRepository;
import com.exceptioncoder.llm.infrastructure.entity.agent.ExecutionStepEntity;
import com.exceptioncoder.llm.infrastructure.entity.agent.ExecutionTraceEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ExecutionTraceRepositoryImpl implements ExecutionTraceRepository {

    private final ExecutionTraceJpaRepository jpaRepository;

    public ExecutionTraceRepositoryImpl(ExecutionTraceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public ExecutionTrace save(ExecutionTrace trace) {
        ExecutionTraceEntity entity = toEntity(trace);
        jpaRepository.save(entity);
        return trace;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExecutionTrace> findByTraceId(String traceId) {
        return jpaRepository.findById(traceId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExecutionTrace> findByAgentId(String agentId, int limit) {
        List<ExecutionTraceEntity> entities = jpaRepository.findTop20ByAgentIdOrderByCreatedAtDesc(agentId);
        return entities.stream().limit(limit).map(this::toDomainWithoutSteps).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExecutionTrace> findRecent(int limit) {
        List<ExecutionTraceEntity> entities = jpaRepository.findTop50ByOrderByCreatedAtDesc();
        return entities.stream().limit(limit).map(this::toDomainWithoutSteps).toList();
    }

    private ExecutionTraceEntity toEntity(ExecutionTrace trace) {
        ExecutionTraceEntity entity = ExecutionTraceEntity.builder()
                .traceId(trace.traceId())
                .agentId(trace.agentId())
                .agentName(trace.agentName())
                .userInput(trace.userInput())
                .finalOutput(trace.finalOutput())
                .status(trace.status())
                .iterations(trace.iterations())
                .elapsedMs(trace.elapsedMs())
                .errorMessage(trace.errorMessage())
                .createdAt(trace.createdAt())
                .build();

        List<ExecutionStepEntity> stepEntities = new ArrayList<>();
        if (trace.steps() != null) {
            for (ExecutionStep step : trace.steps()) {
                ExecutionStepEntity se = ExecutionStepEntity.builder()
                        .trace(entity)
                        .stepOrder(step.stepOrder())
                        .toolId(step.toolId())
                        .toolName(step.toolName())
                        .inputJson(step.inputJson())
                        .outputJson(step.outputJson())
                        .durationMs(step.durationMs())
                        .success(step.success())
                        .errorMessage(step.errorMessage())
                        .build();
                stepEntities.add(se);
            }
        }
        entity.setSteps(stepEntities);
        return entity;
    }

    private ExecutionTrace toDomain(ExecutionTraceEntity entity) {
        List<ExecutionStep> steps = entity.getSteps() != null
                ? entity.getSteps().stream().map(se -> new ExecutionStep(
                        se.getId(), se.getTrace().getTraceId(), se.getStepOrder(),
                        se.getToolId(), se.getToolName(),
                        se.getInputJson(), se.getOutputJson(),
                        se.getDurationMs(), se.isSuccess(), se.getErrorMessage()
                )).toList()
                : List.of();

        return new ExecutionTrace(
                entity.getTraceId(), entity.getAgentId(), entity.getAgentName(),
                entity.getUserInput(), entity.getFinalOutput(),
                entity.getStatus(), entity.getIterations(),
                entity.getElapsedMs(), entity.getErrorMessage(),
                steps, entity.getCreatedAt()
        );
    }

    private ExecutionTrace toDomainWithoutSteps(ExecutionTraceEntity entity) {
        return new ExecutionTrace(
                entity.getTraceId(), entity.getAgentId(), entity.getAgentName(),
                entity.getUserInput(), entity.getFinalOutput(),
                entity.getStatus(), entity.getIterations(),
                entity.getElapsedMs(), entity.getErrorMessage(),
                List.of(), entity.getCreatedAt()
        );
    }
}
