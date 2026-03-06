package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.domain.repository.PromptTemplateRepository;
import com.exceptioncoder.llm.domain.service.PromptTemplate;
import com.exceptioncoder.llm.domain.service.PromptTemplateFactory;
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
 * Prompt 模板管理用例
 * 提供模板的 CRUD 操作，隔离 API 层和 Infrastructure 层
 */
@Slf4j
@Component
public class PromptTemplateManagementUseCase {
    
    private final PromptTemplateRepository templateRepository;
    private final PromptTemplateFactory templateFactory;
    
    public PromptTemplateManagementUseCase(
            PromptTemplateRepository templateRepository,
            PromptTemplateFactory templateFactory) {
        this.templateRepository = templateRepository;
        this.templateFactory = templateFactory;
    }
    
    /**
     * 获取所有启用的模板
     */
    public List<TemplateInfo> getAllTemplates() {
        return templateRepository.findAllEnabled().stream()
                .map(this::convertToInfo)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据名称获取模板
     */
    public Optional<TemplateInfo> getTemplateByName(String name) {
        return templateRepository.findByName(name)
                .map(this::convertToInfo);
    }
    
    /**
     * 根据分类获取模板
     */
    public List<TemplateInfo> getTemplatesByCategory(String category) {
        return templateRepository.findByCategory(category).stream()
                .map(this::convertToInfo)
                .collect(Collectors.toList());
    }
    
    /**
     * 创建或更新模板
     */
    public TemplateInfo saveTemplate(TemplateCreateRequest request) {
        log.info("保存模板: {}", request.getTemplateName());
        
        // 使用工厂创建模板实例
        PromptTemplate template = templateFactory.createTemplate(
                request.getTemplateName(),
                request.getTemplateContent(),
                request.getCategory(),
                request.getDescription(),
                request.getVariableExamples()
        );
        
        PromptTemplate saved = templateRepository.save(template);
        
        return convertToInfo(saved);
    }
    
    /**
     * 获取模板的变量示例
     */
    public Optional<String> getTemplateVariableExamples(String templateName) {
        return templateRepository.findByName(templateName)
                .map(PromptTemplate::getVariableExamples);
    }
    
    /**
     * 删除模板
     */
    public void deleteTemplate(String name) {
        log.info("删除模板: {}", name);
        templateRepository.deleteByName(name);
    }
    
    /**
     * 转换为 TemplateInfo
     */
    private TemplateInfo convertToInfo(PromptTemplate template) {
        return TemplateInfo.builder()
                .templateName(template.getTemplateName())
                .templateContent(template.getTemplateContent())
                .category(template.getCategory())
                .description(template.getDescription())
                .variableExamples(template.getVariableExamples())
                .build();
    }
    
    /**
     * 模板信息 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateInfo {
        private String templateName;
        private String templateContent;
        private String category;
        private String description;
        private String variableExamples;
    }
    
    /**
     * 模板创建请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateCreateRequest {
        private String templateName;
        private String templateContent;
        private String category;
        private String description;
        private String variableExamples;
    }
}

