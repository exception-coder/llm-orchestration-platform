package com.exceptioncoder.llm.api.dto;

import com.exceptioncoder.llm.domain.model.TokenUsage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    
    /**
     * 对话ID
     */
    private String conversationId;
    
    /**
     * AI回复消息
     */
    private String message;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * Token使用情况
     */
    private TokenUsage tokenUsage;
}

