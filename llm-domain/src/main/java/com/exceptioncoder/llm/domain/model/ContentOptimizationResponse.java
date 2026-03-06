package com.exceptioncoder.llm.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 内容优化响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentOptimizationResponse {
    
    /**
     * 优化后的内容
     */
    private String optimizedContent;
    
    /**
     * 建议的标题（多个选项）
     */
    private List<String> suggestedTitles;
    
    /**
     * 建议的标签/话题
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
    private TokenUsage tokenUsage;
}

