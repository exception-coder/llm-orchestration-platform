package com.exceptioncoder.llm.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM 配置
 * 支持动态刷新
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LLMConfiguration {
    
    /**
     * 默认提供商
     */
    private String defaultProvider = "openai";
    
    /**
     * 默认模型
     */
    private String defaultModel = "gpt-3.5-turbo";
    
    /**
     * OpenAI配置
     */
    private OpenAIConfig openai = new OpenAIConfig();
    
    /**
     * Ollama配置
     */
    private OllamaConfig ollama = new OllamaConfig();
    
    /**
     * DeepSeek配置
     */
    private DeepSeekConfig deepseek = new DeepSeekConfig();
    
    /**
     * 通用配置
     */
    private CommonConfig common = new CommonConfig();
    
    @Data
    public static class OpenAIConfig {
        private String apiKey;
        private String baseUrl = "https://api.openai.com";
        private String model = "gpt-3.5-turbo";
        private Double temperature = 0.7;
        private Integer maxTokens = 2000;
    }
    
    @Data
    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434";
        private String model = "llama2";
        private Double temperature = 0.7;
    }
    
    @Data
    public static class DeepSeekConfig {
        private String apiKey;
        private String baseUrl = "https://api.deepseek.com";
        private String model = "deepseek-chat";
        private Double temperature = 0.7;
        private Integer maxTokens = 4000;
    }
    
    @Data
    public static class CommonConfig {
        private Integer timeout = 60000;
        private Integer maxRetries = 3;
        private Map<String, Object> parameters = new HashMap<>();
    }
}

