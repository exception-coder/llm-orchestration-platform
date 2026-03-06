package com.exceptioncoder.llm.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 内容优化响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentOptimizationResponseDTO {
    
    /**
     * 优化后的内容
     */
    private String optimizedContent;
    
    /**
     * 建议的标题
     */
    private List<String> suggestedTitles;
    
    /**
     * 建议的标签
     */
    private List<String> suggestedTags;
    
    /**
     * 优化说明
     */
    private String optimizationNotes;
    
    /**
     * 使用的平台
     */
    private String platform;
    
    /**
     * 使用的风格
     */
    private String style;
    
    /**
     * Token使用情况
     */
    private TokenUsageDTO tokenUsage;
    
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

