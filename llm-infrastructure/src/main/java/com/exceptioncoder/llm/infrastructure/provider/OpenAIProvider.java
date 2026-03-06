package com.exceptioncoder.llm.infrastructure.provider;

import com.exceptioncoder.llm.domain.model.LLMRequest;
import com.exceptioncoder.llm.domain.model.LLMResponse;
import com.exceptioncoder.llm.domain.model.Message;
import com.exceptioncoder.llm.domain.model.TokenUsage;
import com.exceptioncoder.llm.domain.service.LLMProvider;
import com.exceptioncoder.llm.infrastructure.config.LLMConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI Provider 实现
 * 使用 Spring AI
 */
@Slf4j
@Component
public class OpenAIProvider implements LLMProvider {
    
    private final OpenAiChatModel chatModel;
    private final LLMConfiguration configuration;
    
    public OpenAIProvider(OpenAiChatModel chatModel, LLMConfiguration configuration) {
        this.chatModel = chatModel;
        this.configuration = configuration;
    }
    
    @Override
    public LLMResponse chat(LLMRequest request) {
        try {
            Prompt prompt = buildPrompt(request);
            ChatResponse response = chatModel.call(prompt);
            
            return convertResponse(response);
        } catch (Exception e) {
            log.error("OpenAI 调用失败", e);
            throw new RuntimeException("OpenAI 调用失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Flux<String> chatStream(LLMRequest request) {
        try {
            Prompt prompt = buildPrompt(request);
            return chatModel.stream(prompt)
                    .map(response -> response.getResult().getOutput().getContent());
        } catch (Exception e) {
            log.error("OpenAI 流式调用失败", e);
            return Flux.error(new RuntimeException("OpenAI 流式调用失败: " + e.getMessage(), e));
        }
    }
    
    @Override
    public String getProviderName() {
        return "openai";
    }
    
    @Override
    public boolean supports(String model) {
        return model != null && (model.startsWith("gpt-") || model.startsWith("o1-"));
    }
    
    private Prompt buildPrompt(LLMRequest request) {
        List<org.springframework.ai.chat.messages.Message> messages = request.getMessages().stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(request.getModel() != null ? request.getModel() : configuration.getOpenai().getModel())
                .withTemperature(request.getTemperature() != null ? request.getTemperature() : configuration.getOpenai().getTemperature())
                .withMaxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : configuration.getOpenai().getMaxTokens())
                .build();
        
        return new Prompt(messages, options);
    }
    
    private org.springframework.ai.chat.messages.Message convertMessage(Message message) {
        return switch (message.getRole().toLowerCase()) {
            case "system" -> new SystemMessage(message.getContent());
            case "user" -> new UserMessage(message.getContent());
            default -> new UserMessage(message.getContent());
        };
    }
    
    private LLMResponse convertResponse(ChatResponse response) {
        var result = response.getResult();
        var metadata = response.getMetadata();
        
        return LLMResponse.builder()
                .content(result.getOutput().getContent())
                .model(metadata.getModel())
                .tokenUsage(TokenUsage.builder()
                        .promptTokens(metadata.getUsage().getPromptTokens().intValue())
                        .completionTokens(metadata.getUsage().getGenerationTokens().intValue())
                        .totalTokens(metadata.getUsage().getTotalTokens().intValue())
                        .build())
                .finishReason(result.getMetadata().getFinishReason())
                .build();
    }
}

