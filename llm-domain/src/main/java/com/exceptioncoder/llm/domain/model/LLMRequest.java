package com.exceptioncoder.llm.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * LLM 请求领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMRequest {
    
    /**
     * 提示词内容
     */
    private String prompt;
    
    /**
     * 对话历史
     */
    private List<Message> messages;
    
    /**
     * 模型提供商（openai, ollama等）
     */
    private String provider;
    
    /**
     * 具体模型名称
     */
    private String model;
    
    /**
     * 温度参数（0-1）
     */
    private Double temperature;
    
    /**
     * 最大token数
     */
    private Integer maxTokens;
    
    /**
     * 额外参数
     */
    private Map<String, Object> parameters;
    
    /**
     * 是否流式输出
     */
    private Boolean stream;
}

