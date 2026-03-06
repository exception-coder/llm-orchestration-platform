package com.exceptioncoder.llm.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    /**
     * 对话ID
     */
    @NotBlank(message = "对话ID不能为空")
    private String conversationId;
    
    /**
     * 用户消息
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;
    
    /**
     * 提供商（可选）
     */
    private String provider;
    
    /**
     * 模型名称（可选）
     */
    private String model;
    
    /**
     * 温度参数（可选）
     */
    private Double temperature;
    
    /**
     * 最大token数（可选）
     */
    private Integer maxTokens;
    
    /**
     * 是否使用流式输出（可选，默认false）
     */
    private Boolean stream;
}

