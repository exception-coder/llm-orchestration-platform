package com.exceptioncoder.llm.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * LLM 模型配置实体
 * 用于管理可用的模型列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "llm_model_config", indexes = {
        @Index(name = "idx_model_code", columnList = "model_code"),
        @Index(name = "idx_provider", columnList = "provider")
})
public class LLMModelConfigEntity {
    
    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 模型代码（唯一标识）
     */
    @Column(name = "model_code", nullable = false, unique = true, length = 100)
    private String modelCode;
    
    /**
     * 提供商（openai, ollama, deepseek等）
     */
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;
    
    /**
     * 模型显示名称
     */
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;
    
    /**
     * 模型描述
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 创建人
     */
    @Column(name = "created_by", length = 50)
    private String createdBy;
    
    /**
     * 更新人
     */
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}

