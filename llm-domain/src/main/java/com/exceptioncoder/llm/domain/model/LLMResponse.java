package com.exceptioncoder.llm.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * LLM 响应领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMResponse {
    
    /**
     * 生成的内容
     */
    private String content;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * token使用情况
     */
    private TokenUsage tokenUsage;
    
    /**
     * 完成原因
     */
    private String finishReason;
    
    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;
}

