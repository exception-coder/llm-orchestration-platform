package com.exceptioncoder.llm.domain.service;

import com.exceptioncoder.llm.domain.model.JobPosting;

/**
 * 岗位JD向量文本提取服务接口
 */
public interface JobVectorExtractor {
    
    /**
     * 从岗位JD中提取结构化向量文本
     * 
     * @param jobDescription 原始岗位描述
     * @return 结构化向量文本
     */
    String extractVectorText(String jobDescription);
    
    /**
     * 从岗位对象生成向量文本
     * 
     * @param jobPosting 岗位对象
     * @return 向量文本
     */
    String generateVectorText(JobPosting jobPosting);
}

