package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.domain.model.LLMModelConfig;
import com.exceptioncoder.llm.domain.repository.LLMModelConfigRepository;
import com.exceptioncoder.llm.infrastructure.entity.LLMModelConfigEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * LLM 模型配置仓储实现
 */
@Slf4j
@Repository
public class LLMModelConfigRepositoryImpl implements LLMModelConfigRepository {
    
    private final LLMModelConfigJpaRepository jpaRepository;
    
    public LLMModelConfigRepositoryImpl(LLMModelConfigJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public List<LLMModelConfig> findAllEnabled() {
        return jpaRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LLMModelConfig> findByProvider(String provider) {
        return jpaRepository.findByProviderAndEnabledTrueOrderBySortOrderAsc(provider).stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<LLMModelConfig> findByModelCode(String modelCode) {
        return jpaRepository.findByModelCode(modelCode)
                .map(this::convertToDomain);
    }
    
    @Override
    public LLMModelConfig save(LLMModelConfig modelConfig) {
        log.info("保存模型配置: {}", modelConfig.getModelCode());
        
        // 查询是否已存在该模型
        Optional<LLMModelConfigEntity> existingOpt = jpaRepository.findByModelCode(modelConfig.getModelCode());
        
        LLMModelConfigEntity entity;
        if (existingOpt.isPresent()) {
            // 更新现有模型
            log.info("更新现有模型: {}", modelConfig.getModelCode());
            entity = existingOpt.get();
            entity.setProvider(modelConfig.getProvider());
            entity.setModelName(modelConfig.getModelName());
            entity.setDescription(modelConfig.getDescription());
            entity.setEnabled(modelConfig.getEnabled() != null ? modelConfig.getEnabled() : true);
            entity.setSortOrder(modelConfig.getSortOrder() != null ? modelConfig.getSortOrder() : 0);
        } else {
            // 创建新模型
            log.info("创建新模型: {}", modelConfig.getModelCode());
            entity = LLMModelConfigEntity.builder()
                    .modelCode(modelConfig.getModelCode())
                    .provider(modelConfig.getProvider())
                    .modelName(modelConfig.getModelName())
                    .description(modelConfig.getDescription())
                    .enabled(modelConfig.getEnabled() != null ? modelConfig.getEnabled() : true)
                    .sortOrder(modelConfig.getSortOrder() != null ? modelConfig.getSortOrder() : 0)
                    .build();
        }
        
        LLMModelConfigEntity saved = jpaRepository.save(entity);
        return convertToDomain(saved);
    }
    
    @Override
    public void deleteByModelCode(String modelCode) {
        log.info("删除模型配置: {}", modelCode);
        jpaRepository.findByModelCode(modelCode)
                .ifPresent(jpaRepository::delete);
    }
    
    /**
     * 实体转换为领域模型
     */
    private LLMModelConfig convertToDomain(LLMModelConfigEntity entity) {
        return LLMModelConfig.builder()
                .modelCode(entity.getModelCode())
                .provider(entity.getProvider())
                .modelName(entity.getModelName())
                .description(entity.getDescription())
                .enabled(entity.getEnabled())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}

