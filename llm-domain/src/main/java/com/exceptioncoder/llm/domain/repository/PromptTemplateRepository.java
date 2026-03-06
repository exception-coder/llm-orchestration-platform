package com.exceptioncoder.llm.domain.repository;

import com.exceptioncoder.llm.domain.service.PromptTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Prompt 模板仓储接口
 */
public interface PromptTemplateRepository {
    
    /**
     * 根据模板名称查询
     */
    Optional<PromptTemplate> findByName(String templateName);
    
    /**
     * 根据分类查询
     */
    List<PromptTemplate> findByCategory(String category);
    
    /**
     * 查询所有启用的模板
     */
    List<PromptTemplate> findAllEnabled();
    
    /**
     * 保存模板
     */
    PromptTemplate save(PromptTemplate template);
    
    /**
     * 删除模板
     */
    void deleteByName(String templateName);
}

