package com.exceptioncoder.llm.infrastructure.vector;

import com.exceptioncoder.llm.domain.model.JobPosting;
import com.exceptioncoder.llm.domain.model.VectorSearchRequest;
import com.exceptioncoder.llm.domain.model.VectorSearchResult;
import com.exceptioncoder.llm.domain.repository.VectorStoreRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Repository;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 Spring AI 的 Qdrant 向量存储实现
 */
@Repository
public class QdrantVectorStoreRepository implements VectorStoreRepository {
    
    private final VectorStore vectorStore;
    
    public QdrantVectorStoreRepository(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
    
    @Override
    public void store(JobPosting jobPosting, String vectorText) {
        Document document = createDocument(jobPosting, vectorText);
        vectorStore.add(Collections.singletonList(document));
    }
    
    @Override
    public void batchStore(List<JobPosting> jobPostings, List<String> vectorTexts) {
        if (jobPostings.size() != vectorTexts.size()) {
            throw new IllegalArgumentException("岗位数量与向量文本数量不匹配");
        }
        
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < jobPostings.size(); i++) {
            documents.add(createDocument(jobPostings.get(i), vectorTexts.get(i)));
        }
        
        vectorStore.add(documents);
    }
    
    @Override
    public VectorSearchResult search(VectorSearchRequest request) {
        // 构建 Spring AI 搜索请求
        SearchRequest searchRequest = SearchRequest.builder().query(request.getQueryText())
            .topK(request.getTopK()).build();

        // 添加过滤条件
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            // Spring AI 使用 Filter.Expression 进行过滤
            String filterExpression = buildFilterExpression(request.getFilters());
            searchRequest = SearchRequest.builder().query(request.getQueryText())
                .topK(request.getTopK())
                .filterExpression(filterExpression).build();
        }
        
        // 执行搜索
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        
        // 转换结果
        List<VectorSearchResult.ScoredJobPosting> results = documents.stream()
            .map(this::convertToScoredJobPosting)
            .collect(Collectors.toList());
        
        return new VectorSearchResult(results, results.size());
    }
    
    @Override
    public void delete(Long postingId) {
        // Spring AI VectorStore 使用 document ID 删除
        vectorStore.delete(Collections.singletonList(String.valueOf(postingId)));
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // 尝试执行一个简单的搜索来检查连接
            vectorStore.similaritySearch(SearchRequest.builder().query("health_check").topK(1).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 创建 Spring AI Document
     */
    private Document createDocument(JobPosting jobPosting, String vectorText) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("posting_id", jobPosting.getPostingId());
        
        if (jobPosting.getJobFamily() != null) {
            metadata.put("job_family", jobPosting.getJobFamily());
        }
        if (jobPosting.getLevel() != null) {
            metadata.put("level", jobPosting.getLevel());
        }
        if (jobPosting.getCity() != null) {
            metadata.put("city", jobPosting.getCity());
        }
        if (jobPosting.getPostTime() != null) {
            metadata.put("post_time", jobPosting.getPostTime().format(DateTimeFormatter.ISO_DATE));
        }
        if (jobPosting.getDupGroupId() != null) {
            metadata.put("dup_group_id", jobPosting.getDupGroupId());
        }
        
        // 使用 posting_id 作为文档 ID
        return new Document(String.valueOf(jobPosting.getPostingId()), vectorText, metadata);
    }
    
    /**
     * 构建过滤表达式
     * Spring AI 使用类似 SQL 的过滤语法
     */
    private String buildFilterExpression(Map<String, Object> filters) {
        List<String> conditions = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                conditions.add(String.format("%s == '%s'", key, value));
            } else {
                conditions.add(String.format("%s == %s", key, value));
            }
        }
        
        return String.join(" && ", conditions);
    }
    
    /**
     * 转换 Spring AI Document 到领域模型
     */
    private VectorSearchResult.ScoredJobPosting convertToScoredJobPosting(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        
        JobPosting jobPosting = new JobPosting();
        
        if (metadata.containsKey("posting_id")) {
            Object postingId = metadata.get("posting_id");
            if (postingId instanceof Number) {
                jobPosting.setPostingId(((Number) postingId).longValue());
            } else if (postingId instanceof String) {
                jobPosting.setPostingId(Long.parseLong((String) postingId));
            }
        }
        
        if (metadata.containsKey("job_family")) {
            jobPosting.setJobFamily((String) metadata.get("job_family"));
        }
        if (metadata.containsKey("level")) {
            jobPosting.setLevel((String) metadata.get("level"));
        }
        if (metadata.containsKey("city")) {
            jobPosting.setCity((String) metadata.get("city"));
        }
        if (metadata.containsKey("dup_group_id")) {
            jobPosting.setDupGroupId((String) metadata.get("dup_group_id"));
        }
        
        // Spring AI Document 可能包含相似度分数
        // 如果没有，使用默认值
        double score = metadata.containsKey("score") 
            ? ((Number) metadata.get("score")).doubleValue() 
            : 1.0;
        
        return new VectorSearchResult.ScoredJobPosting(jobPosting, score);
    }
}
