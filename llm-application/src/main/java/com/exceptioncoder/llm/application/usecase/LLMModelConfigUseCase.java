package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.domain.model.LLMModelConfig;
import com.exceptioncoder.llm.domain.repository.LLMModelConfigRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * LLM 模型配置管理用例
 */
@Slf4j
@Component
public class LLMModelConfigUseCase {
    
    private final LLMModelConfigRepository modelConfigRepository;
    
    public LLMModelConfigUseCase(LLMModelConfigRepository modelConfigRepository) {
        this.modelConfigRepository = modelConfigRepository;
    }
    
    /**
     * 获取所有启用的模型
     */
    public List<ModelInfo> getAllEnabledModels() {
        return modelConfigRepository.findAllEnabled().stream()
                .map(this::convertToInfo)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有平台（Provider）及其模型数量
     */
    public List<ProviderInfo> getAllProviders() {
        return modelConfigRepository.findAllEnabled().stream()
                .collect(Collectors.groupingBy(LLMModelConfig::getProvider, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ProviderInfo(e.getKey(), e.getValue().intValue()))
                .toList();
    }

    /**
     * 根据提供商获取模型
     */
    public List<ModelInfo> getModelsByProvider(String provider) {
        return modelConfigRepository.findByProvider(provider).stream()
                .map(this::convertToInfo)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据模型代码获取模型
     */
    public Optional<ModelInfo> getModelByCode(String modelCode) {
        return modelConfigRepository.findByModelCode(modelCode)
                .map(this::convertToInfo);
    }
    
    /**
     * 保存模型配置
     */
    public ModelInfo saveModel(ModelCreateRequest request) {
        log.info("保存模型配置: {}", request.getModelCode());
        
        LLMModelConfig modelConfig = LLMModelConfig.builder()
                .modelCode(request.getModelCode())
                .provider(request.getProvider())
                .modelName(request.getModelName())
                .description(request.getDescription())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        
        LLMModelConfig saved = modelConfigRepository.save(modelConfig);
        return convertToInfo(saved);
    }
    
    /**
     * 删除模型配置
     */
    public void deleteModel(String modelCode) {
        log.info("删除模型配置: {}", modelCode);
        modelConfigRepository.deleteByModelCode(modelCode);
    }
    
    private ModelInfo convertToInfo(LLMModelConfig modelConfig) {
        return ModelInfo.builder()
                .modelCode(modelConfig.getModelCode())
                .provider(modelConfig.getProvider())
                .modelName(modelConfig.getModelName())
                .description(modelConfig.getDescription())
                .enabled(modelConfig.getEnabled())
                .sortOrder(modelConfig.getSortOrder())
                .build();
    }
    
    /**
     * 模型信息 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelInfo {
        private String modelCode;
        private String provider;
        private String modelName;
        private String description;
        private Boolean enabled;
        private Integer sortOrder;
    }
    
    /**
     * 平台信息 VO
     */
    public record ProviderInfo(String provider, int modelCount) {}

    /**
     * 模型创建请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelCreateRequest {
        private String modelCode;
        private String provider;
        private String modelName;
        private String description;
        private Boolean enabled;
        private Integer sortOrder;
    }
}

