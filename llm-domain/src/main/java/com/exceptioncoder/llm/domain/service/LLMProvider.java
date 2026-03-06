package com.exceptioncoder.llm.domain.service;

import com.exceptioncoder.llm.domain.model.LLMRequest;
import com.exceptioncoder.llm.domain.model.LLMResponse;
import reactor.core.publisher.Flux;

/**
 * LLM 提供商接口
 * 基础设施层需要实现此接口
 */
public interface LLMProvider {
    
    /**
     * 同步调用
     */
    LLMResponse chat(LLMRequest request);
    
    /**
     * 流式调用
     */
    Flux<String> chatStream(LLMRequest request);
    
    /**
     * 获取提供商名称
     */
    String getProviderName();
    
    /**
     * 是否支持该模型
     */
    boolean supports(String model);
}

