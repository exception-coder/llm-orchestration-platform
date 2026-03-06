package com.exceptioncoder.llm.domain.repository;

import com.exceptioncoder.llm.domain.model.JobPosting;
import com.exceptioncoder.llm.domain.model.VectorSearchRequest;
import com.exceptioncoder.llm.domain.model.VectorSearchResult;

/**
 * 向量存储仓储接口
 */
public interface VectorStoreRepository {
    
    /**
     * 存储岗位向量
     * 
     * @param jobPosting 岗位信息
     * @param vectorText 向量文本（用于向量化）
     */
    void store(JobPosting jobPosting, String vectorText);
    
    /**
     * 批量存储岗位向量
     * 
     * @param jobPostings 岗位列表
     * @param vectorTexts 对应的向量文本列表
     */
    void batchStore(java.util.List<JobPosting> jobPostings, java.util.List<String> vectorTexts);
    
    /**
     * 向量相似度检索
     * 
     * @param request 检索请求
     * @return 检索结果
     */
    VectorSearchResult search(VectorSearchRequest request);
    
    /**
     * 删除岗位向量
     * 
     * @param postingId 岗位ID
     */
    void delete(Long postingId);
    
    /**
     * 检查向量库连接状态
     * 
     * @return 是否连接正常
     */
    boolean isHealthy();
}

