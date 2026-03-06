package com.exceptioncoder.llm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Prompt 测试请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTestRequestDTO {
    
    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    
    /**
     * 模板变量
     */
    @NotNull(message = "模板变量不能为空")
    private Map<String, Object> variables;
    
    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空")
    private String model;
    
    /**
     * 提供商（openai/ollama）
     */
    private String provider;
    
    /**
     * 温度参数（0.0-2.0）
     */
    private Double temperature;
    
    /**
     * 最大 token 数
     */
    private Integer maxTokens;
}

