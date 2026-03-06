package com.exceptioncoder.llm.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM 模型配置领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMModelConfig {
    
    /**
     * 模型代码（唯一标识）
     */
    private String modelCode;
    
    /**
     * 提供商（openai, ollama, deepseek等）
     */
    private String provider;
    
    /**
     * 模型显示名称
     */
    private String modelName;
    
    /**
     * 模型描述
     */
    private String description;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
}

