package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.domain.repository.PromptTemplateRepository;
import com.exceptioncoder.llm.domain.service.PromptTemplate;
import com.exceptioncoder.llm.infrastructure.entity.PromptTemplateEntity;
import com.exceptioncoder.llm.infrastructure.prompt.DatabasePromptTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Prompt 模板仓储实现
 * 使用缓存提升性能
 */
@Slf4j
@Repository
public class PromptTemplateRepositoryImpl implements PromptTemplateRepository {
    
    private final PromptTemplateJpaRepository jpaRepository;
    
    public PromptTemplateRepositoryImpl(PromptTemplateJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    @Cacheable(value = "promptTemplates", key = "#templateName")
    public Optional<PromptTemplate> findByName(String templateName) {
        log.debug("从数据库查询模板: {}", templateName);
        return jpaRepository.findByTemplateNameAndEnabledTrue(templateName)
                .map(this::convertToPromptTemplate);
    }
    
    @Override
    @Cacheable(value = "promptTemplatesByCategory", key = "#category")
    public List<PromptTemplate> findByCategory(String category) {
        log.debug("从数据库查询分类模板: {}", category);
        return jpaRepository.findByCategoryAndEnabledTrue(category).stream()
                .map(this::convertToPromptTemplate)
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = "allPromptTemplates")
    public List<PromptTemplate> findAllEnabled() {
        log.debug("从数据库查询所有启用的模板");
        return jpaRepository.findByEnabledTrue().stream()
                .map(this::convertToPromptTemplate)
                .collect(Collectors.toList());
    }
    
    @Override
    @CacheEvict(value = {"promptTemplates", "promptTemplatesByCategory", "allPromptTemplates"}, allEntries = true)
    public PromptTemplate save(PromptTemplate template) {
        log.info("保存模板: {}", template.getTemplateName());
        
        DatabasePromptTemplate dbTemplate = (DatabasePromptTemplate) template;
        
        // 查询是否已存在该模板
        Optional<PromptTemplateEntity> existingOpt = jpaRepository.findByTemplateNameAndEnabledTrue(dbTemplate.getTemplateName());
        
        PromptTemplateEntity entity;
        if (existingOpt.isPresent()) {
            // 更新现有模板
            log.info("更新现有模板: {}", dbTemplate.getTemplateName());
            entity = existingOpt.get();
            entity.setTemplateContent(dbTemplate.getTemplateContent());
            entity.setCategory(dbTemplate.getCategory());
            entity.setDescription(dbTemplate.getDescription());
            entity.setVariableExamples(dbTemplate.getVariableExamples());
            entity.setVersion(entity.getVersion() + 1); // 版本号递增
        } else {
            // 创建新模板
            log.info("创建新模板: {}", dbTemplate.getTemplateName());
            entity = PromptTemplateEntity.builder()
                    .templateName(dbTemplate.getTemplateName())
                    .templateContent(dbTemplate.getTemplateContent())
                    .category(dbTemplate.getCategory())
                    .description(dbTemplate.getDescription())
                    .variableExamples(dbTemplate.getVariableExamples())
                    .enabled(true)
                    .version(1)
                    .build();
        }
        
        PromptTemplateEntity saved = jpaRepository.save(entity);
        return convertToPromptTemplate(saved);
    }
    
    @Override
    @CacheEvict(value = {"promptTemplates", "promptTemplatesByCategory", "allPromptTemplates"}, allEntries = true)
    public void deleteByName(String templateName) {
        log.info("删除模板: {}", templateName);
        jpaRepository.findByTemplateNameAndEnabledTrue(templateName)
                .ifPresent(entity -> {
                    entity.setEnabled(false);
                    jpaRepository.save(entity);
                });
    }
    
    /**
     * 实体转换为领域模型
     */
    private PromptTemplate convertToPromptTemplate(PromptTemplateEntity entity) {
        return DatabasePromptTemplate.builder()
                .templateName(entity.getTemplateName())
                .templateContent(entity.getTemplateContent())
                .category(entity.getCategory())
                .description(entity.getDescription())
                .variableExamples(entity.getVariableExamples())
                .build();
    }
}

