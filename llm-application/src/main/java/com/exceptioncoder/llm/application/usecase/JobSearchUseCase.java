package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.domain.model.VectorSearchRequest;
import com.exceptioncoder.llm.domain.model.VectorSearchResult;
import com.exceptioncoder.llm.domain.repository.VectorStoreRepository;
import org.springframework.stereotype.Component;

/**
 * 岗位检索用例
 */
@Component
public class JobSearchUseCase {
    
    private final VectorStoreRepository vectorStoreRepository;
    
    public JobSearchUseCase(VectorStoreRepository vectorStoreRepository) {
        this.vectorStoreRepository = vectorStoreRepository;
    }
    
    /**
     * 执行岗位相似度检索
     */
    public VectorSearchResult execute(VectorSearchRequest request) {
        validateRequest(request);
        return vectorStoreRepository.search(request);
    }
    
    /**
     * 带过滤条件的检索
     */
    public VectorSearchResult searchWithFilters(String queryText, int topK, 
                                                String jobFamily, String level, String city) {
        VectorSearchRequest request = new VectorSearchRequest(queryText, topK);
        
        if (jobFamily != null && !jobFamily.isEmpty()) {
            request.addFilter("job_family", jobFamily);
        }
        if (level != null && !level.isEmpty()) {
            request.addFilter("level", level);
        }
        if (city != null && !city.isEmpty()) {
            request.addFilter("city", city);
        }
        
        return execute(request);
    }
    
    private void validateRequest(VectorSearchRequest request) {
        if (request.getQueryText() == null || request.getQueryText().trim().isEmpty()) {
            throw new IllegalArgumentException("查询文本不能为空");
        }
        if (request.getTopK() <= 0) {
            throw new IllegalArgumentException("TopK必须大于0");
        }
        if (request.getTopK() > 100) {
            throw new IllegalArgumentException("TopK不能超过100");
        }
    }
}

