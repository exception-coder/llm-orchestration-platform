package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.infrastructure.entity.LLMModelConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * LLM 模型配置 JPA Repository
 */
@Repository
public interface LLMModelConfigJpaRepository extends JpaRepository<LLMModelConfigEntity, Long> {
    
    /**
     * 查找所有启用的模型，按排序顺序
     */
    List<LLMModelConfigEntity> findByEnabledTrueOrderBySortOrderAsc();
    
    /**
     * 根据提供商查找启用的模型
     */
    List<LLMModelConfigEntity> findByProviderAndEnabledTrueOrderBySortOrderAsc(String provider);
    
    /**
     * 根据模型代码查找
     */
    Optional<LLMModelConfigEntity> findByModelCode(String modelCode);
}

