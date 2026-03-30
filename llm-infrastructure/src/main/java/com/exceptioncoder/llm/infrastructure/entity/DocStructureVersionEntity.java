package com.exceptioncoder.llm.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 文档目录结构版本 JPA 实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "doc_structure_version", indexes = {
        @Index(name = "idx_dsv_active", columnList = "is_active"),
        @Index(name = "idx_dsv_version", columnList = "version")
})
public class DocStructureVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version", nullable = false, unique = true)
    private int version;

    @Column(name = "structure", columnDefinition = "LONGTEXT", nullable = false)
    private String structure;

    @Column(name = "diff_summary", length = 1000)
    private String diffSummary;

    @Column(name = "readme_hash", length = 64)
    private String readmeHash;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
