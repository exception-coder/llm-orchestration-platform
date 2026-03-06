package com.exceptioncoder.llm.domain.service;

import com.exceptioncoder.llm.domain.model.LLMRequest;
import com.exceptioncoder.llm.domain.model.LLMResponse;

/**
 * Chain 执行器接口
 * 用于编排多个LLM调用
 */
public interface ChainExecutor {
    
    /**
     * 执行Chain
     */
    LLMResponse execute(LLMRequest request);
    
    /**
     * 获取Chain名称
     */
    String getChainName();
}

