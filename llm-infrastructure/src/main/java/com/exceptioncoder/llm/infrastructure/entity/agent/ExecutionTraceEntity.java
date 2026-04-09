package com.exceptioncoder.llm.infrastructure.entity.agent;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行流水主表实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "execution_trace", indexes = {
        @Index(name = "idx_trace_agent_id", columnList = "agent_id"),
        @Index(name = "idx_trace_created_at", columnList = "created_at")
})
public class ExecutionTraceEntity {

    @Id
    @Column(name = "trace_id", length = 36)
    private String traceId;

    @Column(name = "agent_id", length = 100)
    private String agentId;

    @Column(name = "agent_name", length = 200)
    private String agentName;

    @Column(name = "user_input", columnDefinition = "TEXT")
    private String userInput;

    @Column(name = "final_output", columnDefinition = "TEXT")
    private String finalOutput;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "iterations")
    private int iterations;

    @Column(name = "elapsed_ms")
    private long elapsedMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "trace", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    private List<ExecutionStepEntity> steps;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
