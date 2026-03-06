package com.exceptioncoder.llm.application.service;

import com.exceptioncoder.llm.domain.model.JobPosting;
import com.exceptioncoder.llm.domain.model.VectorSearchRequest;
import com.exceptioncoder.llm.domain.model.VectorSearchResult;
import com.exceptioncoder.llm.domain.repository.VectorStoreRepository;
import com.exceptioncoder.llm.domain.service.JobVectorExtractor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 岗位向量服务
 * 负责协调向量提取和存储
 */
@Service
public class JobVectorService {
    
    private final VectorStoreRepository vectorStoreRepository;
    private final JobVectorExtractor jobVectorExtractor;
    
    public JobVectorService(VectorStoreRepository vectorStoreRepository,
                           JobVectorExtractor jobVectorExtractor) {
        this.vectorStoreRepository = vectorStoreRepository;
        this.jobVectorExtractor = jobVectorExtractor;
    }
    
    /**
     * 存储岗位到向量库
     */
    public void storeJobPosting(JobPosting jobPosting) {
        String vectorText = jobVectorExtractor.generateVectorText(jobPosting);
        vectorStoreRepository.store(jobPosting, vectorText);
    }
    
    /**
     * 批量存储岗位
     */
    public void batchStoreJobPostings(List<JobPosting> jobPostings) {
        List<String> vectorTexts = jobPostings.stream()
            .map(jobVectorExtractor::generateVectorText)
            .collect(Collectors.toList());
        
        vectorStoreRepository.batchStore(jobPostings, vectorTexts);
    }
    
    /**
     * 从原始JD提取并存储
     */
    public void storeFromJobDescription(Long postingId, String jobDescription, JobPosting metadata) {
        String vectorText = jobVectorExtractor.extractVectorText(jobDescription);
        metadata.setPostingId(postingId);
        vectorStoreRepository.store(metadata, vectorText);
    }
    
    /**
     * 删除岗位向量
     */
    public void deleteJobPosting(Long postingId) {
        vectorStoreRepository.delete(postingId);
    }
    
    /**
     * 检查向量库健康状态
     */
    public boolean checkHealth() {
        return vectorStoreRepository.isHealthy();
    }
}

