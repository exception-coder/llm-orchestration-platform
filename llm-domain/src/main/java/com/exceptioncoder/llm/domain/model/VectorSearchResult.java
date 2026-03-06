package com.exceptioncoder.llm.domain.model;

import java.util.List;

/**
 * 向量检索结果
 */
public class VectorSearchResult {
    
    private List<ScoredJobPosting> results;
    private int totalCount;
    
    public VectorSearchResult() {
    }
    
    public VectorSearchResult(List<ScoredJobPosting> results, int totalCount) {
        this.results = results;
        this.totalCount = totalCount;
    }
    
    public List<ScoredJobPosting> getResults() {
        return results;
    }
    
    public void setResults(List<ScoredJobPosting> results) {
        this.results = results;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    
    /**
     * 带相似度分数的岗位
     */
    public static class ScoredJobPosting {
        private JobPosting jobPosting;
        private double score;
        
        public ScoredJobPosting() {
        }
        
        public ScoredJobPosting(JobPosting jobPosting, double score) {
            this.jobPosting = jobPosting;
            this.score = score;
        }
        
        public JobPosting getJobPosting() {
            return jobPosting;
        }
        
        public void setJobPosting(JobPosting jobPosting) {
            this.jobPosting = jobPosting;
        }
        
        public double getScore() {
            return score;
        }
        
        public void setScore(double score) {
            this.score = score;
        }
    }
}

