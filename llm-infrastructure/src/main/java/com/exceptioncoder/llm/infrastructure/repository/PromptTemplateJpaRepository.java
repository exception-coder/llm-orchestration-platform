package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.infrastructure.entity.PromptTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Prompt 模板 JPA Repository
 */
@Repository
public interface PromptTemplateJpaRepository extends JpaRepository<PromptTemplateEntity, Long> {
    
    /**
     * 根据模板名称查询
     */
    Optional<PromptTemplateEntity> findByTemplateNameAndEnabledTrue(String templateName);
    
    /**
     * 根据分类查询
     */
    List<PromptTemplateEntity> findByCategoryAndEnabledTrue(String category);
    
    /**
     * 查询所有启用的模板
     */
    List<PromptTemplateEntity> findByEnabledTrue();
}

