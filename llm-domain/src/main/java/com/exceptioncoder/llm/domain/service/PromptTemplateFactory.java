package com.exceptioncoder.llm.domain.service;

/**
 * Prompt 模板工厂接口
 * 用于创建模板实例，隔离 API/Application 层和 Infrastructure 层
 */
public interface PromptTemplateFactory {
    
    /**
     * 创建模板实例
     * 
     * @param templateName 模板名称
     * @param templateContent 模板内容
     * @param category 分类
     * @param description 描述
     * @return 模板实例
     */
    PromptTemplate createTemplate(
            String templateName,
            String templateContent,
            String category,
            String description
    );
    
    /**
     * 创建模板实例（带变量示例）
     * 
     * @param templateName 模板名称
     * @param templateContent 模板内容
     * @param category 分类
     * @param description 描述
     * @param variableExamples 变量示例（JSON格式）
     * @return 模板实例
     */
    PromptTemplate createTemplate(
            String templateName,
            String templateContent,
            String category,
            String description,
            String variableExamples
    );
}

