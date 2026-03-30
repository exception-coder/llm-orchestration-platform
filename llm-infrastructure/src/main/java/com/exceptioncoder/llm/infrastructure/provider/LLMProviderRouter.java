package com.exceptioncoder.llm.infrastructure.provider;

import com.exceptioncoder.llm.domain.model.ModelType;
import com.exceptioncoder.llm.domain.service.LLMProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LLM Provider 路由器
 * 统一管理所有 Provider，上层通过此类路由，不直接依赖具体 Provider
 */
@Slf4j
@Component
public class LLMProviderRouter {

    private final List<LLMProvider> providers;

    public LLMProviderRouter(List<LLMProvider> providers) {
        this.providers = providers;
    }

    /**
     * 按 ModelType 路由
     */
    public LLMProvider route(ModelType type) {
        return providers.stream()
                .filter(p -> p.getProviderName().equals(type.getProviderName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No provider found for ModelType: " + type));
    }

    /**
     * 按 model 字符串路由
     */
    public LLMProvider route(String model) {
        return providers.stream()
                .filter(p -> p.supports(model))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No provider supports model: " + model));
    }
}
