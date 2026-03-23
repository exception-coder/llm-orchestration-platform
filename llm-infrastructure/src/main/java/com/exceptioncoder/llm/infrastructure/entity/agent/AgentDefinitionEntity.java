package com.exceptioncoder.llm.infrastructure.entity.agent;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Agent 定义实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "agent_definition", indexes = {
        @Index(name = "idx_agent_enabled", columnList = "enabled")
})
public class AgentDefinitionEntity {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    /** 工具 ID 列表（JSON 数组字符串） */
    @Column(name = "tool_ids", columnDefinition = "TEXT")
    private String toolIds;

    @Column(name = "llm_provider", length = 100)
    private String llmProvider;

    @Column(name = "llm_model", length = 100)
    private String llmModel;

    @Column(name = "max_iterations")
    private Integer maxIterations;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
