package com.exceptioncoder.llm.domain.model;

public enum ModelType {
    ALI("alibaba"),
    OLLAMA("ollama"),
    ZHIPU("zhipu");

    private final String providerName;

    ModelType(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }
}
