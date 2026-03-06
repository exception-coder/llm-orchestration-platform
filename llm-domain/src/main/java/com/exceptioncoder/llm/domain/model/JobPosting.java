package com.exceptioncoder.llm.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * 岗位领域模型
 */
public class JobPosting {
    
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
    
    public JobPosting() {
    }
    
    public JobPosting(Long postingId, String jobFamily, String level, List<String> skills,
                     String experience, String education, String domain, String responsibility,
                     String city, LocalDate postTime, String dupGroupId) {
        this.postingId = postingId;
        this.jobFamily = jobFamily;
        this.level = level;
        this.skills = skills;
        this.experience = experience;
        this.education = education;
        this.domain = domain;
        this.responsibility = responsibility;
        this.city = city;
        this.postTime = postTime;
        this.dupGroupId = dupGroupId;
    }
    
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
}

