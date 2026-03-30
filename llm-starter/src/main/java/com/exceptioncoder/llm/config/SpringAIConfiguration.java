package com.exceptioncoder.llm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI 配置类
 * 解决多个 EmbeddingModel Bean 冲突问题
 * 
 * 问题描述：
 * 当同时配置了 OpenAI 和 Ollama 时，Spring AI 会创建两个 EmbeddingModel bean。
 * QdrantVectorStoreAutoConfiguration 需要注入一个 EmbeddingModel，但不知道使用哪个。
 * 
 * EmbeddingModel 说明：
 * - 作用：将文本转换为向量（embedding），用于向量存储和相似度检索
 * - OpenAI EmbeddingModel：调用远程 API（https://api.openai.com），需要 API 密钥
 * - Ollama EmbeddingModel：调用本地 HTTP 服务（http://localhost:11434），无需外部 API
 * - 两者都需要网络请求（HTTP API 调用），只是服务位置不同
 * 
 * 解决方案：
 * 创建 DelegatingEmbeddingModel 代理类，统一管理多个 EmbeddingModel
 * 
 * 优势：
 * 1. 支持多个 Embedding 模型共存
 * 2. 根据模型标识符动态选择具体实现
 * 3. 提供默认模型（OpenAI）
 * 4. 对外透明，符合 EmbeddingModel 接口
 * 
 * 使用方式：
 * - 默认使用 OpenAI Embedding
 * - 通过 DelegatingEmbeddingModel.setCurrentModel("ollama") 切换模型
 * - 通过 delegatingEmbeddingModel.getModel("ollama") 获取特定模型
 * 
 * @author system
 */
@Configuration
public class SpringAIConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(SpringAIConfiguration.class);
    
    /**
     * 创建 DelegatingEmbeddingModel 代理 Bean
     * 标记为 @Primary，作为主要的 EmbeddingModel
     * 
     * @param openAiEmbeddingModel OpenAI Embedding 模型（可选）
     * @param ollamaEmbeddingModel Ollama Embedding 模型（可选）
     * @return DelegatingEmbeddingModel 代理实例
     */
    @Bean
    @Primary
    public DelegatingEmbeddingModel delegatingEmbeddingModel(
            @Autowired(required = false) OllamaEmbeddingModel ollamaEmbeddingModel) {
        
        log.info("创建 DelegatingEmbeddingModel 代理");
        
        DelegatingEmbeddingModel delegatingModel = new DelegatingEmbeddingModel(
            ollamaEmbeddingModel
        );
        
        log.info("可用的 Embedding 模型: {}", delegatingModel.getAvailableModels());
        
        return delegatingModel;
    }
}

