package com.exceptioncoder.llm.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token使用统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsage {
    
    /**
     * 提示词token数
     */
    private Integer promptTokens;
    
    /**
     * 生成token数
     */
    private Integer completionTokens;
    
    /**
     * 总token数
     */
    private Integer totalTokens;
}

