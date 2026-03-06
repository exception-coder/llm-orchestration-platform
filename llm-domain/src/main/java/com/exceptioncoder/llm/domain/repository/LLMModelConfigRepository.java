package com.exceptioncoder.llm.domain.repository;

import com.exceptioncoder.llm.domain.model.LLMModelConfig;

import java.util.List;
import java.util.Optional;

/**
 * LLM 模型配置仓储接口
 */
public interface LLMModelConfigRepository {
    
    /**
     * 查找所有启用的模型
     */
    List<LLMModelConfig> findAllEnabled();
    
    /**
     * 根据提供商查找启用的模型
     */
    List<LLMModelConfig> findByProvider(String provider);
    
    /**
     * 根据模型代码查找
     */
    Optional<LLMModelConfig> findByModelCode(String modelCode);
    
    /**
     * 保存模型配置
     */
    LLMModelConfig save(LLMModelConfig modelConfig);
    
    /**
     * 删除模型配置
     */
    void deleteByModelCode(String modelCode);
}

