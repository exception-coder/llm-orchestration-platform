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
 * Prompt 模板实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "prompt_template", indexes = {
        @Index(name = "idx_template_name", columnList = "template_name"),
        @Index(name = "idx_category", columnList = "category")
})
public class PromptTemplateEntity {
    
    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 模板名称（唯一）
     */
    @Column(name = "template_name", nullable = false, unique = true, length = 100)
    private String templateName;
    
    /**
     * 模板分类
     */
    @Column(name = "category", length = 50)
    private String category;
    
    /**
     * 模板内容
     */
    @Column(name = "template_content", nullable = false, columnDefinition = "TEXT")
    private String templateContent;
    
    /**
     * 模板描述
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 模板变量（JSON格式，记录需要的变量）
     */
    @Column(name = "variables", columnDefinition = "TEXT")
    private String variables;
    
    /**
     * 变量示例（JSON格式，用于前端测试时的默认值）
     */
    @Column(name = "variable_examples", columnDefinition = "TEXT")
    private String variableExamples;
    
    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    /**
     * 版本号
     */
    @Column(name = "version", nullable = false)
    private Integer version = 1;
    
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

