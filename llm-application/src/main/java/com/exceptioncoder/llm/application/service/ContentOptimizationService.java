package com.exceptioncoder.llm.application.service;

import com.exceptioncoder.llm.domain.model.ContentOptimizationRequest;
import com.exceptioncoder.llm.domain.model.ContentOptimizationResponse;
import com.exceptioncoder.llm.domain.model.LLMRequest;
import com.exceptioncoder.llm.domain.model.LLMResponse;
import com.exceptioncoder.llm.domain.model.Message;
import com.exceptioncoder.llm.domain.repository.PromptTemplateRepository;
import com.exceptioncoder.llm.domain.service.PromptTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容优化服务
 * 负责编排内容优化的完整流程
 */
@Slf4j
@Service
public class ContentOptimizationService {
    
    private final LLMOrchestrationService orchestrationService;
    private final PromptTemplateRepository promptTemplateRepository;
    private final ObjectMapper objectMapper;
    
    public ContentOptimizationService(
            LLMOrchestrationService orchestrationService,
            PromptTemplateRepository promptTemplateRepository) {
        this.orchestrationService = orchestrationService;
        this.promptTemplateRepository = promptTemplateRepository;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 优化内容
     */
    public ContentOptimizationResponse optimize(ContentOptimizationRequest request) {
        log.info("开始优化内容，平台: {}, 风格: {}", 
                request.getPlatform().getDisplayName(), 
                request.getStyle().getDisplayName());
        
        // 1. 从数据库获取模板
        PromptTemplate template = promptTemplateRepository.findByName("content-optimization")
                .orElseThrow(() -> new IllegalStateException("未找到内容优化模板，请先初始化数据库"));
        
        // 2. 构建 Prompt
        String prompt = buildPrompt(request, template);
        
        // 3. 调用 LLM
        LLMRequest llmRequest = LLMRequest.builder()
                .prompt(prompt)
                .messages(List.of(Message.builder()
                        .role("user")
                        .content(prompt)
                        .build()))
                .model(request.getModel() != null ? request.getModel() : "gpt-3.5-turbo")
                .temperature(0.8) // 提高创造性
                .maxTokens(2000)
                .build();
        
        LLMResponse llmResponse = orchestrationService.chat(llmRequest);
        
        // 4. 解析响应
        ContentOptimizationResponse response = parseResponse(llmResponse, request);
        
        log.info("内容优化完成，使用模型: {}", llmResponse.getModel());
        return response;
    }
    
    /**
     * 批量优化（生成多个版本）
     */
    public List<ContentOptimizationResponse> optimizeMultiple(
            ContentOptimizationRequest request, int count) {
        List<ContentOptimizationResponse> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(optimize(request));
        }
        return results;
    }
    
    /**
     * 构建 Prompt
     */
    private String buildPrompt(ContentOptimizationRequest request, PromptTemplate template) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("platformCharacteristics", request.getPlatform().getCharacteristics());
        variables.put("styleDescription", request.getStyle().getDescription());
        variables.put("originalContent", request.getOriginalContent());
        variables.put("contentType", request.getContentType() != null ? 
                request.getContentType().getDisplayName() : "通用内容");
        variables.put("targetAudience", request.getTargetAudience() != null ? 
                request.getTargetAudience() : "通用受众");
        variables.put("additionalRequirements", request.getAdditionalRequirements() != null ? 
                request.getAdditionalRequirements() : "无");
        
        return template.render(variables);
    }
    
    /**
     * 解析 LLM 响应
     */
    private ContentOptimizationResponse parseResponse(
            LLMResponse llmResponse, ContentOptimizationRequest request) {
        try {
            String content = llmResponse.getContent().trim();
            
            // 移除可能的 markdown 代码块标记
            if (content.startsWith("```json")) {
                content = content.substring(7);
            }
            if (content.startsWith("```")) {
                content = content.substring(3);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();
            
            JsonNode jsonNode = objectMapper.readTree(content);
            
            List<String> titles = new ArrayList<>();
            if (jsonNode.has("suggestedTitles")) {
                jsonNode.get("suggestedTitles").forEach(node -> titles.add(node.asText()));
            }
            
            List<String> tags = new ArrayList<>();
            if (jsonNode.has("suggestedTags")) {
                jsonNode.get("suggestedTags").forEach(node -> tags.add(node.asText()));
            }
            
            return ContentOptimizationResponse.builder()
                    .optimizedContent(jsonNode.get("optimizedContent").asText())
                    .suggestedTitles(titles)
                    .suggestedTags(tags)
                    .optimizationNotes(jsonNode.has("optimizationNotes") ? 
                            jsonNode.get("optimizationNotes").asText() : "")
                    .platform(request.getPlatform().getDisplayName())
                    .style(request.getStyle().getDisplayName())
                    .tokenUsage(llmResponse.getTokenUsage())
                    .build();
                    
        } catch (Exception e) {
            log.error("解析响应失败，返回原始内容", e);
            // 如果解析失败，返回基础响应
            return ContentOptimizationResponse.builder()
                    .optimizedContent(llmResponse.getContent())
                    .suggestedTitles(List.of())
                    .suggestedTags(List.of())
                    .optimizationNotes("响应解析失败，返回原始内容")
                    .platform(request.getPlatform().getDisplayName())
                    .style(request.getStyle().getDisplayName())
                    .tokenUsage(llmResponse.getTokenUsage())
                    .build();
        }
    }
}

