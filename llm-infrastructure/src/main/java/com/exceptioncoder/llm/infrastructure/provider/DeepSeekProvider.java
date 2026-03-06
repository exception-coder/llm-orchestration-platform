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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DeepSeek Provider 实现
 * DeepSeek API 兼容 OpenAI 格式，使用 Spring AI 的 OpenAI 客户端
 */
@Slf4j
@Component
@Primary
public class DeepSeekProvider implements LLMProvider {
    
    private final LLMConfiguration configuration;
    private OpenAiChatModel chatModel;
    
    public DeepSeekProvider(LLMConfiguration configuration) {
        this.configuration = configuration;
    }
    
    @Override
    public LLMResponse chat(LLMRequest request) {
        try {
            OpenAiChatModel model = getChatModel();
            Prompt prompt = buildPrompt(request);
            ChatResponse response = model.call(prompt);
            
            return convertResponse(response);
        } catch (Exception e) {
            log.error("DeepSeek 调用失败", e);
            throw new RuntimeException("DeepSeek 调用失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Flux<String> chatStream(LLMRequest request) {
        try {
            OpenAiChatModel model = getChatModel();
            Prompt prompt = buildPrompt(request);
            return model.stream(prompt)
                    .map(response -> response.getResult().getOutput().getContent());
        } catch (Exception e) {
            log.error("DeepSeek 流式调用失败", e);
            return Flux.error(new RuntimeException("DeepSeek 流式调用失败: " + e.getMessage(), e));
        }
    }
    
    @Override
    public String getProviderName() {
        return "deepseek";
    }
    
    @Override
    public boolean supports(String model) {
        return model != null && model.toLowerCase().contains("deepseek");
    }
    
    /**
     * 获取或创建 ChatModel
     * DeepSeek 使用 OpenAI 兼容的 API
     */
    private synchronized OpenAiChatModel getChatModel() {
        if (chatModel == null) {
            LLMConfiguration.DeepSeekConfig config = configuration.getDeepseek();
            
            OpenAiApi openAiApi = new OpenAiApi(
                    config.getBaseUrl(),
                    config.getApiKey()
            );
            
            chatModel = new OpenAiChatModel(openAiApi, 
                    OpenAiChatOptions.builder()
                            .withModel(config.getModel())
                            .withTemperature(config.getTemperature())
                            .withMaxTokens(config.getMaxTokens())
                            .build()
            );
        }
        return chatModel;
    }
    
    private Prompt buildPrompt(LLMRequest request) {
        List<org.springframework.ai.chat.messages.Message> messages = request.getMessages().stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());
        
        LLMConfiguration.DeepSeekConfig config = configuration.getDeepseek();
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(request.getModel() != null ? request.getModel() : config.getModel())
                .withTemperature(request.getTemperature() != null ? request.getTemperature() : config.getTemperature())
                .withMaxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens())
                .build();
        
        return new Prompt(messages, options);
    }
    
    private org.springframework.ai.chat.messages.Message convertMessage(Message message) {
        return switch (message.getRole().toLowerCase()) {
            case "system" -> new SystemMessage(message.getContent());
            case "user" -> new UserMessage(message.getContent());
            case "assistant" -> new AssistantMessage(message.getContent());
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

