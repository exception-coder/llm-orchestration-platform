package com.exceptioncoder.llm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EmbeddingModel 代理类
 * 支持多个 Embedding 模型，根据模型标识符动态选择。
 *
 * <p>默认使用 OpenAI 兼容模型（智谱 embedding-3），Ollama 作为备选。
 *
 * @author zhangkai
 */
public class DelegatingEmbeddingModel implements EmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(DelegatingEmbeddingModel.class);

    private static final ThreadLocal<String> CURRENT_MODEL = new ThreadLocal<>();

    private final Map<String, EmbeddingModel> modelRegistry = new HashMap<>();
    private final EmbeddingModel defaultModel;

    public DelegatingEmbeddingModel(EmbeddingModel defaultModel, Map<String, EmbeddingModel> models) {
        this.defaultModel = defaultModel;
        this.modelRegistry.putAll(models);
        log.info("DelegatingEmbeddingModel 初始化完成，可用模型: {}", modelRegistry.keySet());
    }

    public static void setCurrentModel(String modelIdentifier) {
        CURRENT_MODEL.set(modelIdentifier);
    }

    public static void clearCurrentModel() {
        CURRENT_MODEL.remove();
    }

    private EmbeddingModel getCurrentModel() {
        String modelIdentifier = CURRENT_MODEL.get();
        if (modelIdentifier != null) {
            EmbeddingModel model = modelRegistry.get(modelIdentifier);
            if (model != null) {
                return model;
            }
            log.warn("未找到指定的 EmbeddingModel: {}，使用默认模型", modelIdentifier);
        }
        return defaultModel;
    }

    public EmbeddingModel getModel(String modelIdentifier) {
        if (modelIdentifier == null || modelIdentifier.isEmpty()) {
            return defaultModel;
        }
        return modelRegistry.getOrDefault(modelIdentifier, defaultModel);
    }

    public List<String> getAvailableModels() {
        return List.copyOf(modelRegistry.keySet());
    }

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
