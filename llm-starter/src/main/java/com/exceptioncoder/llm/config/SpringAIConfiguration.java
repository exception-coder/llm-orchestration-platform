package com.exceptioncoder.llm.config;

import com.exceptioncoder.llm.infrastructure.config.LLMConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring AI 配置类
 * 创建 DelegatingEmbeddingModel 代理，优先使用智谱（手动构建 OpenAI 兼容客户端）做 Embedding。
 *
 * <p>spring.ai.openai 配置位留给 OpenAI 原生使用，智谱通过 llm.zhipu 自定义配置手动构建。
 *
 * @author zhangkai
 */
@Configuration
public class SpringAIConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SpringAIConfiguration.class);

    /**
     * 智谱 Embedding 模型（手动构建，走 OpenAI 兼容协议）
     */
    @Bean
    public OpenAiEmbeddingModel zhipuEmbeddingModel(LLMConfiguration llmConfiguration) {
        LLMConfiguration.ZhipuConfig zhipu = llmConfiguration.getZhipu();
        if (zhipu.getApiKey() == null || zhipu.getApiKey().isBlank()) {
            log.warn("智谱 API Key 未配置，zhipuEmbeddingModel 不可用");
            return null;
        }

        OpenAiApi api = OpenAiApi.builder()
                .apiKey(zhipu.getApiKey())
                .baseUrl(zhipu.getBaseUrl())
                .embeddingsPath("/v4/embeddings")
                .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(zhipu.getEmbeddingModel())
                .build();

        log.info("创建智谱 EmbeddingModel: base-url={}, model={}",
                zhipu.getBaseUrl(), zhipu.getEmbeddingModel());
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
    }

    @Bean
    @Primary
    public DelegatingEmbeddingModel delegatingEmbeddingModel(
            @Autowired(required = false) OpenAiEmbeddingModel zhipuEmbeddingModel,
            @Autowired(required = false) OllamaEmbeddingModel ollamaEmbeddingModel) {

        Map<String, EmbeddingModel> models = new LinkedHashMap<>();
        EmbeddingModel defaultModel = null;

        // 智谱优先
        if (zhipuEmbeddingModel != null) {
            models.put("zhipu", zhipuEmbeddingModel);
            defaultModel = zhipuEmbeddingModel;
            log.info("注册智谱 EmbeddingModel [默认]");
        }

        if (ollamaEmbeddingModel != null) {
            models.put("ollama", ollamaEmbeddingModel);
            if (defaultModel == null) {
                defaultModel = ollamaEmbeddingModel;
                log.info("注册 Ollama EmbeddingModel [默认]");
            } else {
                log.info("注册 Ollama EmbeddingModel [备选]");
            }
        }

        if (defaultModel == null) {
            throw new IllegalStateException("至少需要配置一个 EmbeddingModel（智谱 或 Ollama）");
        }

        return new DelegatingEmbeddingModel(defaultModel, models);
    }
}
