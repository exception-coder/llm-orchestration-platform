package com.exceptioncoder.llm.infrastructure.provider;

import com.exceptioncoder.llm.domain.model.LLMRequest;
import com.exceptioncoder.llm.domain.model.LLMResponse;
import com.exceptioncoder.llm.domain.model.TokenUsage;
import com.exceptioncoder.llm.domain.service.LLMProvider;
import com.exceptioncoder.llm.infrastructure.config.LLMConfiguration;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ollama Provider 实现
 * 使用 LangChain4j
 */
@Slf4j
@Component
public class OllamaProvider implements LLMProvider {
    
    private final LLMConfiguration configuration;
    
    public OllamaProvider(LLMConfiguration configuration) {
        this.configuration = configuration;
    }
    
    @Override
    public LLMResponse chat(LLMRequest request) {
        try {
            ChatLanguageModel model = buildModel(request);
            List<ChatMessage> messages = convertMessages(request);
            
            Response<AiMessage> response = model.generate(messages);
            
            return convertResponse(response, request.getModel());
        } catch (Exception e) {
            log.error("Ollama 调用失败", e);
            throw new RuntimeException("Ollama 调用失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Flux<String> chatStream(LLMRequest request) {
        // Ollama 流式调用需要使用 StreamingChatLanguageModel
        // 这里简化处理，返回完整响应
        return Flux.just(chat(request).getContent());
    }
    
    @Override
    public String getProviderName() {
        return "ollama";
    }
    
    @Override
    public boolean supports(String model) {
        return model != null && (model.contains("llama") || model.contains("mistral") || 
                model.contains("qwen") || model.contains("deepseek"));
    }
    
    private ChatLanguageModel buildModel(LLMRequest request) {
        return OllamaChatModel.builder()
                .baseUrl(configuration.getOllama().getBaseUrl())
                .modelName(request.getModel() != null ? request.getModel() : configuration.getOllama().getModel())
                .temperature(request.getTemperature() != null ? request.getTemperature() : configuration.getOllama().getTemperature())
                .timeout(Duration.ofMillis(configuration.getCommon().getTimeout()))
                .build();
    }
    
    private List<ChatMessage> convertMessages(LLMRequest request) {
        return request.getMessages().stream()
                .map(msg -> {
                    return switch (msg.getRole().toLowerCase()) {
                        case "system" -> dev.langchain4j.data.message.SystemMessage.from(msg.getContent());
                        case "user" -> dev.langchain4j.data.message.UserMessage.from(msg.getContent());
                        case "assistant" -> dev.langchain4j.data.message.AiMessage.from(msg.getContent());
                        default -> dev.langchain4j.data.message.UserMessage.from(msg.getContent());
                    };
                })
                .collect(Collectors.toList());
    }
    
    private LLMResponse convertResponse(Response<AiMessage> response, String model) {
        return LLMResponse.builder()
                .content(response.content().text())
                .model(model)
                .tokenUsage(TokenUsage.builder()
                        .promptTokens(response.tokenUsage() != null ? response.tokenUsage().inputTokenCount() : 0)
                        .completionTokens(response.tokenUsage() != null ? response.tokenUsage().outputTokenCount() : 0)
                        .totalTokens(response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : 0)
                        .build())
                .finishReason(response.finishReason() != null ? response.finishReason().toString() : null)
                .build();
    }
}

