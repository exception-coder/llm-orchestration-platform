package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.application.service.LLMOrchestrationService;
import com.exceptioncoder.llm.domain.model.LLMRequest;
import com.exceptioncoder.llm.domain.model.LLMResponse;
import com.exceptioncoder.llm.domain.model.Message;
import com.exceptioncoder.llm.domain.repository.PromptTemplateRepository;
import com.exceptioncoder.llm.domain.service.PromptTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Prompt 测试用例
 * 用于在前端验证不同模板和模型的输出效果
 */
@Slf4j
@Component
public class PromptTestUseCase {
    
    private final PromptTemplateRepository templateRepository;
    private final LLMOrchestrationService orchestrationService;
    
    public PromptTestUseCase(
            PromptTemplateRepository templateRepository,
            LLMOrchestrationService orchestrationService) {
        this.templateRepository = templateRepository;
        this.orchestrationService = orchestrationService;
    }
    
    /**
     * 执行 Prompt 测试
     * 
     * @param templateName 模板名称
     * @param variables 变量映射
     * @param model 模型名称
     * @param provider 提供商（可选）
     * @param temperature 温度参数（可选）
     * @param maxTokens 最大token数（可选）
     * @return 测试结果
     */
    public PromptTestResult execute(
            String templateName,
            Map<String, Object> variables,
            String model,
            String provider,
            Double temperature,
            Integer maxTokens) {
        
        long startTime = System.currentTimeMillis();
        
        // 1. 获取模板
        PromptTemplate template = templateRepository.findByName(templateName)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + templateName));
        
        // 2. 渲染模板
        String renderedPrompt = template.render(variables);
        log.info("渲染模板 [{}]，长度: {} 字符", templateName, renderedPrompt.length());
        
        // 3. 构建 LLM 请求
        LLMRequest llmRequest = LLMRequest.builder()
                .prompt(renderedPrompt)
                .messages(List.of(Message.builder()
                        .role("user")
                        .content(renderedPrompt)
                        .build()))
                .model(model)
                .provider(provider)
                .temperature(temperature != null ? temperature : 0.7)
                .maxTokens(maxTokens != null ? maxTokens : 2000)
                .stream(false)
                .build();
        
        // 4. 调用 LLM
        LLMResponse llmResponse = orchestrationService.chat(llmRequest);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        log.info("Prompt 测试完成，模型: {}, 耗时: {}ms", llmResponse.getModel(), executionTime);
        
        // 5. 返回结果
        return PromptTestResult.builder()
                .renderedPrompt(renderedPrompt)
                .output(llmResponse.getContent())
                .model(llmResponse.getModel())
                .tokenUsage(llmResponse.getTokenUsage())
                .executionTime(executionTime)
                .build();
    }
    
    /**
     * Prompt 测试结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PromptTestResult {
        private String renderedPrompt;
        private String output;
        private String model;
        private com.exceptioncoder.llm.domain.model.TokenUsage tokenUsage;
        private Long executionTime;
    }
}

