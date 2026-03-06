package com.exceptioncoder.llm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EmbeddingModel 代理类
 * 支持多个 Embedding 模型，根据模型标识符动态选择
 * 
 * <p>设计目标：
 * <ul>
 *   <li>统一的 EmbeddingModel 接口，对外透明</li>
 *   <li>支持多个 Embedding 提供商（OpenAI、Ollama 等）</li>
 *   <li>根据模型标识符动态路由到具体实现</li>
 *   <li>提供默认模型（OpenAI）</li>
 * </ul>
 * 
 * <p>使用方式：
 * <pre>
 * // 方式 1：使用默认模型（OpenAI）
 * delegatingEmbeddingModel.embed("文本");
 * 
 * // 方式 2：通过 ThreadLocal 切换模型
 * try {
 *     DelegatingEmbeddingModel.setCurrentModel("ollama");
 *     delegatingEmbeddingModel.embed("文本");
 * } finally {
 *     DelegatingEmbeddingModel.clearCurrentModel();
 * }
 * 
 * // 方式 3：直接获取特定模型
 * EmbeddingModel ollamaModel = delegatingEmbeddingModel.getModel("ollama");
 * ollamaModel.embed("文本");
 * 
 * // 方式 4：查询可用模型
 * List&lt;String&gt; models = delegatingEmbeddingModel.getAvailableModels();
 * </pre>
 * 
 * @author system
 */
public class DelegatingEmbeddingModel implements EmbeddingModel {
    
    private static final Logger log = LoggerFactory.getLogger(DelegatingEmbeddingModel.class);
    
    /**
     * 默认模型标识符
     */
    private static final String DEFAULT_MODEL = "openai";
    
    /**
     * 当前线程使用的模型标识符
     */
    private static final ThreadLocal<String> CURRENT_MODEL = new ThreadLocal<>();
    
    /**
     * 模型注册表：模型标识符 -> EmbeddingModel 实例
     */
    private final Map<String, EmbeddingModel> modelRegistry = new HashMap<>();
    
    /**
     * 默认的 EmbeddingModel
     */
    private final EmbeddingModel defaultModel;
    
    /**
     * 构造函数
     * 
     * @param openAiEmbeddingModel OpenAI Embedding 模型（可选）
     * @param ollamaEmbeddingModel Ollama Embedding 模型（可选）
     */
    public DelegatingEmbeddingModel(
            OpenAiEmbeddingModel openAiEmbeddingModel,
            OllamaEmbeddingModel ollamaEmbeddingModel) {
        
        // 注册 OpenAI 模型
        if (openAiEmbeddingModel != null) {
            modelRegistry.put("openai", openAiEmbeddingModel);
            log.info("注册 OpenAI EmbeddingModel");
        }
        
        // 注册 Ollama 模型
        if (ollamaEmbeddingModel != null) {
            modelRegistry.put("ollama", ollamaEmbeddingModel);
            log.info("注册 Ollama EmbeddingModel");
        }
        
        // 设置默认模型（优先 OpenAI）
        if (openAiEmbeddingModel != null) {
            this.defaultModel = openAiEmbeddingModel;
            log.info("默认 EmbeddingModel: OpenAI");
        } else if (ollamaEmbeddingModel != null) {
            this.defaultModel = ollamaEmbeddingModel;
            log.info("默认 EmbeddingModel: Ollama");
        } else {
            throw new IllegalStateException("至少需要配置一个 EmbeddingModel");
        }
    }
    
    /**
     * 设置当前线程使用的模型
     * 
     * @param modelIdentifier 模型标识符（openai、ollama 等）
     */
    public static void setCurrentModel(String modelIdentifier) {
        CURRENT_MODEL.set(modelIdentifier);
        log.debug("设置当前线程使用的 EmbeddingModel: {}", modelIdentifier);
    }
    
    /**
     * 清除当前线程的模型设置
     */
    public static void clearCurrentModel() {
        CURRENT_MODEL.remove();
    }
    
    /**
     * 获取当前应该使用的 EmbeddingModel
     */
    private EmbeddingModel getCurrentModel() {
        String modelIdentifier = CURRENT_MODEL.get();
        
        if (modelIdentifier != null) {
            EmbeddingModel model = modelRegistry.get(modelIdentifier);
            if (model != null) {
                log.debug("使用指定的 EmbeddingModel: {}", modelIdentifier);
                return model;
            } else {
                log.warn("未找到指定的 EmbeddingModel: {}，使用默认模型", modelIdentifier);
            }
        }
        
        return defaultModel;
    }
    
    /**
     * 根据模型标识符获取 EmbeddingModel
     * 
     * @param modelIdentifier 模型标识符
     * @return EmbeddingModel 实例
     */
    public EmbeddingModel getModel(String modelIdentifier) {
        if (modelIdentifier == null || modelIdentifier.isEmpty()) {
            return defaultModel;
        }
        
        EmbeddingModel model = modelRegistry.get(modelIdentifier);
        if (model == null) {
            log.warn("未找到指定的 EmbeddingModel: {}，使用默认模型", modelIdentifier);
            return defaultModel;
        }
        
        return model;
    }
    
    /**
     * 获取所有已注册的模型标识符
     */
    public List<String> getAvailableModels() {
        return List.copyOf(modelRegistry.keySet());
    }
    
    // ==================== EmbeddingModel 接口实现 ====================
    
    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        return getCurrentModel().call(request);
    }
    
    @Override
    public float[] embed(String text) {
        return getCurrentModel().embed(text);
    }
    
    @Override
    public float[] embed(Document document) {
        return getCurrentModel().embed(document);
    }
    
    @Override
    public List<float[]> embed(List<String> texts) {
        return getCurrentModel().embed(texts);
    }
    
    @Override
    public EmbeddingResponse embedForResponse(List<String> texts) {
        return getCurrentModel().embedForResponse(texts);
    }
    
    @Override
    public int dimensions() {
        return getCurrentModel().dimensions();
    }
}

