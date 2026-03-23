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
 * Graph 定义实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "graph_definition", indexes = {
        @Index(name = "idx_graph_enabled", columnList = "enabled")
})
public class GraphDefinitionEntity {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    /** 节点列表（JSON 数组字符串） */
    @Column(name = "nodes", columnDefinition = "TEXT")
    private String nodes;

    /** 边列表（JSON 数组字符串） */
    @Column(name = "edges", columnDefinition = "TEXT")
    private String edges;

    @Column(name = "entry_node_id", length = 100)
    private String entryNodeId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
