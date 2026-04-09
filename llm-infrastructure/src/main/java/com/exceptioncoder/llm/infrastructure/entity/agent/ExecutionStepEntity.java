package com.exceptioncoder.llm.infrastructure.entity.agent;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行步骤明细实体 -- 单次 Tool 调用记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "execution_step", indexes = {
        @Index(name = "idx_step_trace_id", columnList = "trace_id")
})
public class ExecutionStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trace_id", nullable = false)
    private ExecutionTraceEntity trace;

    @Column(name = "step_order")
    private int stepOrder;

    @Column(name = "tool_id", length = 100)
    private String toolId;

    @Column(name = "tool_name", length = 200)
    private String toolName;

    @Column(name = "input_json", columnDefinition = "TEXT")
    private String inputJson;

    @Column(name = "output_json", columnDefinition = "TEXT")
    private String outputJson;

    @Column(name = "duration_ms")
    private long durationMs;

    @Column(name = "success")
    private boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
