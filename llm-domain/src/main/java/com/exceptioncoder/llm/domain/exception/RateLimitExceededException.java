package com.exceptioncoder.llm.domain.exception;

/**
 * LLM 平台请求频率超限异常
 * 当 Provider 主动限速等待超时或平台返回 429 时抛出
 */
public class RateLimitExceededException extends RuntimeException {

    private final String providerName;

    public RateLimitExceededException(String providerName) {
        super("Provider [" + providerName + "] 请求频率超限");
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }
}
