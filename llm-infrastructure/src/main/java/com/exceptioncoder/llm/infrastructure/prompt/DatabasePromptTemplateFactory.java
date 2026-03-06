package com.exceptioncoder.llm.infrastructure.prompt;

import com.exceptioncoder.llm.domain.service.PromptTemplate;
import com.exceptioncoder.llm.domain.service.PromptTemplateFactory;
import org.springframework.stereotype.Component;

/**
 * 数据库 Prompt 模板工厂实现
 */
@Component
public class DatabasePromptTemplateFactory implements PromptTemplateFactory {
    
    @Override
    public PromptTemplate createTemplate(
            String templateName,
            String templateContent,
            String category,
            String description) {
        
        return DatabasePromptTemplate.builder()
                .templateName(templateName)
                .templateContent(templateContent)
                .category(category)
                .description(description)
                .build();
    }
    
    @Override
    public PromptTemplate createTemplate(
            String templateName,
            String templateContent,
            String category,
            String description,
            String variableExamples) {
        
        return DatabasePromptTemplate.builder()
                .templateName(templateName)
                .templateContent(templateContent)
                .category(category)
                .description(description)
                .variableExamples(variableExamples)
                .build();
    }
}

