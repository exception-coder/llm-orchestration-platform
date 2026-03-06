package com.exceptioncoder.llm.domain.service;

import java.util.Map;

/**
 * Prompt 模板接口
 */
public interface PromptTemplate {
    
    /**
     * 渲染模板
     * 
     * @param variables 变量映射
     * @return 渲染后的 Prompt
     */
    String render(Map<String, Object> variables);
    
    /**
     * 获取模板名称
     */
    String getTemplateName();
    
    /**
     * 获取模板内容
     */
    String getTemplateContent();
    
    /**
     * 获取模板分类
     */
    String getCategory();
    
    /**
     * 获取模板描述
     */
    String getDescription();
    
    /**
     * 获取变量示例（JSON格式）
     */
    String getVariableExamples();
}

