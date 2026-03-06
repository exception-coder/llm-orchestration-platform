package com.exceptioncoder.llm.api.dto;

import java.util.Map;

/**
 * 岗位检索请求DTO
 */
public class JobSearchRequestDTO {
    
    private String queryText;
    private Integer topK = 10;
    private String jobFamily;
    private String level;
    private String city;
    private Map<String, Object> filters;
    
    public String getQueryText() {
        return queryText;
    }
    
    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }
    
    public Integer getTopK() {
        return topK;
    }
    
    public void setTopK(Integer topK) {
        this.topK = topK;
    }
    
    public String getJobFamily() {
        return jobFamily;
    }
    
    public void setJobFamily(String jobFamily) {
        this.jobFamily = jobFamily;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public Map<String, Object> getFilters() {
        return filters;
    }
    
    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }
}

