package com.exceptioncoder.llm.domain.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 向量检索请求
 */
public class VectorSearchRequest {
    
    private String queryText;
    private int topK;
    private Map<String, Object> filters;
    
    public VectorSearchRequest() {
        this.filters = new HashMap<>();
    }
    
    public VectorSearchRequest(String queryText, int topK) {
        this.queryText = queryText;
        this.topK = topK;
        this.filters = new HashMap<>();
    }
    
    public VectorSearchRequest(String queryText, int topK, Map<String, Object> filters) {
        this.queryText = queryText;
        this.topK = topK;
        this.filters = filters != null ? filters : new HashMap<>();
    }
    
    public String getQueryText() {
        return queryText;
    }
    
    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }
    
    public int getTopK() {
        return topK;
    }
    
    public void setTopK(int topK) {
        this.topK = topK;
    }
    
    public Map<String, Object> getFilters() {
        return filters;
    }
    
    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }
    
    public void addFilter(String key, Object value) {
        this.filters.put(key, value);
    }
}

