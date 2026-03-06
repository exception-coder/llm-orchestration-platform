package com.exceptioncoder.llm.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 岗位检索响应DTO
 */
public class JobSearchResponseDTO {
    
    private List<ScoredJobDTO> results;
    private int totalCount;
    
    public JobSearchResponseDTO() {
    }
    
    public JobSearchResponseDTO(List<ScoredJobDTO> results, int totalCount) {
        this.results = results;
        this.totalCount = totalCount;
    }
    
    public List<ScoredJobDTO> getResults() {
        return results;
    }
    
    public void setResults(List<ScoredJobDTO> results) {
        this.results = results;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    
    /**
     * 带相似度分数的岗位DTO
     */
    public static class ScoredJobDTO {
        private Long postingId;
        private String jobFamily;
        private String level;
        private List<String> skills;
        private String experience;
        private String education;
        private String domain;
        private String responsibility;
        private String city;
        private LocalDate postTime;
        private String dupGroupId;
        private double score;
        
        public Long getPostingId() {
            return postingId;
        }
        
        public void setPostingId(Long postingId) {
            this.postingId = postingId;
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
        
        public List<String> getSkills() {
            return skills;
        }
        
        public void setSkills(List<String> skills) {
            this.skills = skills;
        }
        
        public String getExperience() {
            return experience;
        }
        
        public void setExperience(String experience) {
            this.experience = experience;
        }
        
        public String getEducation() {
            return education;
        }
        
        public void setEducation(String education) {
            this.education = education;
        }
        
        public String getDomain() {
            return domain;
        }
        
        public void setDomain(String domain) {
            this.domain = domain;
        }
        
        public String getResponsibility() {
            return responsibility;
        }
        
        public void setResponsibility(String responsibility) {
            this.responsibility = responsibility;
        }
        
        public String getCity() {
            return city;
        }
        
        public void setCity(String city) {
            this.city = city;
        }
        
        public LocalDate getPostTime() {
            return postTime;
        }
        
        public void setPostTime(LocalDate postTime) {
            this.postTime = postTime;
        }
        
        public String getDupGroupId() {
            return dupGroupId;
        }
        
        public void setDupGroupId(String dupGroupId) {
            this.dupGroupId = dupGroupId;
        }
        
        public double getScore() {
            return score;
        }
        
        public void setScore(double score) {
            this.score = score;
        }
    }
}

