package com.exceptioncoder.llm.infrastructure.vector;

import com.exceptioncoder.llm.domain.model.JobPosting;
import com.exceptioncoder.llm.domain.service.JobVectorExtractor;
import com.exceptioncoder.llm.domain.model.LLMRequest;
import com.exceptioncoder.llm.domain.model.LLMResponse;
import com.exceptioncoder.llm.domain.model.Message;
import com.exceptioncoder.llm.infrastructure.provider.LLMProviderRouter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 基于LLM的岗位向量文本提取器
 */
@Component
public class LLMJobVectorExtractor implements JobVectorExtractor {
    
    private final LLMProviderRouter providerRouter;

    private static final String EXTRACTION_PROMPT_TEMPLATE = """
        请从以下岗位JD中提取关键信息，并按照指定格式输出：
        
        岗位JD：
        %s
        
        请提取以下信息并按此格式输出：
        岗位族：{{job_family}}
        级别：{{level}}
        核心技能：{{skills_joined}}
        经验要求：{{exp}}
        学历要求：{{edu}}
        业务领域：{{domain}}
        职责摘要：{{responsibility_summary}}
        
        注意：
        1. 如果某项信息无法从JD中提取，请填写"未知"
        2. 核心技能用逗号分隔
        3. 职责摘要控制在100字以内
        """;
    
    public LLMJobVectorExtractor(LLMProviderRouter providerRouter) {
        this.providerRouter = providerRouter;
    }
    
    @Override
    public String extractVectorText(String jobDescription) {
        String prompt = String.format(EXTRACTION_PROMPT_TEMPLATE, jobDescription);
        
        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent(prompt);
        
        LLMRequest request = new LLMRequest();
        request.setMessages(Collections.singletonList(userMessage));
        request.setTemperature(0.3);
        request.setMaxTokens(500);
        
        LLMResponse response = providerRouter.getDefault().chat(request);
        return response.getContent();
    }
    
    @Override
    public String generateVectorText(JobPosting jobPosting) {
        String skillsJoined = jobPosting.getSkills() != null 
            ? String.join(", ", jobPosting.getSkills())
            : "未知";
        
        return String.format("""
            岗位族：%s
            级别：%s
            核心技能：%s
            经验要求：%s
            学历要求：%s
            业务领域：%s
            职责摘要：%s
            """,
            jobPosting.getJobFamily() != null ? jobPosting.getJobFamily() : "未知",
            jobPosting.getLevel() != null ? jobPosting.getLevel() : "未知",
            skillsJoined,
            jobPosting.getExperience() != null ? jobPosting.getExperience() : "未知",
            jobPosting.getEducation() != null ? jobPosting.getEducation() : "未知",
            jobPosting.getDomain() != null ? jobPosting.getDomain() : "未知",
            jobPosting.getResponsibility() != null ? jobPosting.getResponsibility() : "未知"
        );
    }
}

