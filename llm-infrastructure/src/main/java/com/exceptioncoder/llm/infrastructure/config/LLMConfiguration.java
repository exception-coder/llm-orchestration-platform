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
    private String defaultProvider = "zhipu";

    /**
     * 默认模型
     */
    private String defaultModel = "glm-5.1";

    /**
     * 阿里云百炼配置（通义千问 / DeepSeek 等）
     */
    private AlibabaConfig alibaba = new AlibabaConfig();

    /**
     * Ollama 本地模型配置
     */
    private OllamaConfig ollama = new OllamaConfig();

    /**
     * 智谱 AI 配置（兼容 OpenAI 协议，用于 Embedding）
     */
    private ZhipuConfig zhipu = new ZhipuConfig();

    /**
     * 通用配置
     */
    private CommonConfig common = new CommonConfig();

    @Data
    public static class AlibabaConfig {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com";
        private String model = "qwen-plus";
        private Double temperature = 0.7;
        private Integer maxTokens = 4000;
    }

    @Data
    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434";
        private String model = "llama3.2";
        private Double temperature = 0.7;
    }

    @Data
    public static class ZhipuConfig {
        private String apiKey;
        private String baseUrl = "https://open.bigmodel.cn/api/paas";
        private String embeddingModel = "embedding-3";
        private String model = "glm-4-flash";
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
