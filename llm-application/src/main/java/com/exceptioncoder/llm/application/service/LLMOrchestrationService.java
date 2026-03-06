package com.exceptioncoder.llm.application.service;

import com.exceptioncoder.llm.domain.model.LLMRequest;
import com.exceptioncoder.llm.domain.model.LLMResponse;
import com.exceptioncoder.llm.domain.service.LLMProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * LLM 编排服务
 * 负责路由请求到合适的Provider
 */
@Slf4j
@Service
public class LLMOrchestrationService {
    
    private final Map<String, LLMProvider> providerMap;
    
    public LLMOrchestrationService(List<LLMProvider> providers) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(
                        LLMProvider::getProviderName,
                        Function.identity()
                ));
        log.info("已加载 {} 个 LLM Provider: {}", providerMap.size(), providerMap.keySet());
    }
    
    /**
     * 同步调用
     */
    public LLMResponse chat(LLMRequest request) {
        LLMProvider provider = selectProvider(request);
        log.info("使用 Provider: {} 处理请求", provider.getProviderName());
        return provider.chat(request);
    }
    
    /**
     * 流式调用
     */
    public Flux<String> chatStream(LLMRequest request) {
        LLMProvider provider = selectProvider(request);
        log.info("使用 Provider: {} 处理流式请求", provider.getProviderName());
        return provider.chatStream(request);
    }
    
    /**
     * 选择合适的Provider
     */
    private LLMProvider selectProvider(LLMRequest request) {
        String providerName = request.getProvider();
        
        if (providerName != null && providerMap.containsKey(providerName)) {
            return providerMap.get(providerName);
        }
        
        // 根据模型自动选择Provider
        String model = request.getModel();
        if (model != null) {
            for (LLMProvider provider : providerMap.values()) {
                if (provider.supports(model)) {
                    return provider;
                }
            }
        }
        
        throw new IllegalArgumentException("未找到合适的 LLM Provider");
    }
}

