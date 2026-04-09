package com.exceptioncoder.llm.infrastructure.provider;

import com.exceptioncoder.llm.domain.model.LLMRequest;
import com.exceptioncoder.llm.domain.model.LLMResponse;
import com.exceptioncoder.llm.domain.model.Message;
import com.exceptioncoder.llm.domain.model.TokenUsage;
import com.exceptioncoder.llm.domain.service.LLMProvider;
import com.exceptioncoder.llm.infrastructure.config.LLMConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Ollama Provider 实现
 * 使用 Spring AI Alibaba 提供的 Ollama 集成（原 LangChain4j 方案已废弃）
 */
@Slf4j
@Component
public class OllamaProvider implements LLMProvider {

    private final LLMConfiguration configuration;
    private OllamaChatModel chatModel;

    public OllamaProvider(LLMConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public LLMResponse chat(LLMRequest request) {
        try {
            OllamaChatModel model = getOrCreateChatModel(request);
            Prompt prompt = buildPrompt(request);
            ChatResponse response = model.call(prompt);
            return convertResponse(response, request.getModel());
        } catch (Exception e) {
            log.error("Ollama 调用失败", e);
            throw new RuntimeException("Ollama 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> chatStream(LLMRequest request) {
        try {
            OllamaChatModel model = getOrCreateChatModel(request);
            Prompt prompt = buildPrompt(request);
            return model.stream(prompt)
                    .map(response -> response.getResults().get(0).getOutput().getText());
        } catch (Exception e) {
            log.error("Ollama 流式调用失败", e);
            return Flux.error(new RuntimeException("Ollama 流式调用失败: " + e.getMessage(), e));
        }
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public ChatModel getChatModel() {
        return getOrCreateChatModel();
    }

    @Override
    public boolean supports(String model) {
        if (model == null) return false;
        String lower = model.toLowerCase();
        return lower.contains("llama") ||
               lower.contains("mistral") ||
               lower.contains("qwen") ||
               lower.contains("deepseek") ||
               lower.contains("phi") ||
               lower.contains("gemma") ||
               lower.contains("codellama") ||
               lower.contains("nomic");
    }

    private synchronized OllamaChatModel getOrCreateChatModel() {
        if (chatModel == null) {
            LLMConfiguration.OllamaConfig config = configuration.getOllama();
            OllamaOptions options = OllamaOptions.builder()
                    .model(config.getModel())
                    .temperature(config.getTemperature())
                    .build();
            chatModel = OllamaChatModel.builder()
                    .ollamaApi(OllamaApi.builder().baseUrl(config.getBaseUrl()).build())
                    .defaultOptions(options)
                    .build();
        }
        return chatModel;
    }

    private synchronized OllamaChatModel getOrCreateChatModel(LLMRequest request) {
        if (chatModel == null) {
            LLMConfiguration.OllamaConfig config = configuration.getOllama();
            OllamaOptions options = OllamaOptions.builder()
                    .model(request.getModel() != null ? request.getModel() : config.getModel())
                    .temperature(request.getTemperature() != null ? request.getTemperature() : config.getTemperature())
                    .build();
            chatModel = OllamaChatModel.builder()
                    .ollamaApi(OllamaApi.builder().baseUrl(config.getBaseUrl()).build())
                    .defaultOptions(options)
                    .build();
        }
        return chatModel;
    }

    private Prompt buildPrompt(LLMRequest request) {
        List<org.springframework.ai.chat.messages.Message> messages = request.getMessages().stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());

        LLMConfiguration.OllamaConfig config = configuration.getOllama();
        OllamaOptions options = OllamaOptions.builder()
                .model(request.getModel() != null ? request.getModel() : config.getModel())
                .temperature(request.getTemperature() != null ? request.getTemperature() : config.getTemperature())
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

    private LLMResponse convertResponse(ChatResponse response, String requestedModel) {
        org.springframework.ai.chat.model.Generation result = (org.springframework.ai.chat.model.Generation) response.getResult();
        var metadata = response.getMetadata();

        String model = metadata.getModel();
        if (model == null || model.isEmpty()) {
            model = requestedModel != null ? requestedModel : configuration.getOllama().getModel();
        }

        TokenUsage tokenUsage = null;
        if (metadata.getUsage() != null) {
            var usage = metadata.getUsage();
            tokenUsage = TokenUsage.builder()
                    .promptTokens(usage.getPromptTokens() != null ? usage.getPromptTokens() : 0)
                    .completionTokens(usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0)
                    .totalTokens(usage.getTotalTokens() != null ? usage.getTotalTokens() : 0)
                    .build();
        }

        return LLMResponse.builder()
                .content(response.getResults().get(0).getOutput().getText())
                .model(model)
                .tokenUsage(tokenUsage)
                .finishReason(result.getMetadata().getFinishReason())
                .build();
    }
}
