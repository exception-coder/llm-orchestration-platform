package com.exceptioncoder.llm.infrastructure.entity.devplan;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 项目画像索引状态实体 -- 记录 ProjectProfile 各维度在 Qdrant 向量库中的索引情况。
 *
 * <p>每条记录对应一个"项目×维度"的索引状态，通过 projectPath + dimension 唯一标识。
 * 与 Qdrant 中 project_profile collection 的向量记录形成关联：
 * MySQL 记录索引元数据（状态、hash、时间），Qdrant 存储实际向量。
 *
 * @author zhangkai
 * @since 2026-04-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "profile_index_status", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_dimension", columnNames = {"project_path", "dimension"})
}, indexes = {
        @Index(name = "idx_project_path", columnList = "project_path"),
        @Index(name = "idx_project_name", columnList = "project_name"),
        @Index(name = "idx_status", columnList = "status")
})
public class ProfileIndexStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_path", nullable = false, length = 500)
    private String projectPath;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Column(name = "dimension", nullable = false, length = 30)
    private String dimension;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "last_indexed_at")
    private LocalDateTime lastIndexedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
