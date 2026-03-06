package com.exceptioncoder.llm.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Prompt 测试响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTestResponseDTO {
    
    /**
     * 渲染后的 Prompt
     */
    private String renderedPrompt;
    
    /**
     * LLM 输出结果
     */
    private String output;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * 使用的提供商
     */
    private String provider;
    
    /**
     * Token 使用情况
     */
    private TokenUsageDTO tokenUsage;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long executionTime;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsageDTO {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}

