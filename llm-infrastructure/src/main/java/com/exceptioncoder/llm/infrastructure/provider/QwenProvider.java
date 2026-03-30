package com.exceptioncoder.llm.infrastructure.provider;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.exceptioncoder.llm.domain.model.LLMRequest;
import com.exceptioncoder.llm.domain.model.LLMResponse;
import com.exceptioncoder.llm.domain.model.Message;
import com.exceptioncoder.llm.domain.model.TokenUsage;
import com.exceptioncoder.llm.domain.service.LLMProvider;
import com.exceptioncoder.llm.infrastructure.config.LLMConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通义千问 Provider 实现
 * 使用 Spring AI Alibaba
 */
@Slf4j
@Component
public class QwenProvider implements LLMProvider {

    private final LLMConfiguration configuration;
    private final ObjectMapper objectMapper;
    private DashScopeChatModel chatModel;

    public QwenProvider(LLMConfiguration configuration) {
        this.configuration = configuration;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public LLMResponse chat(LLMRequest request) {
        try {
            DashScopeChatModel model = getChatModel();
            Prompt prompt = buildPrompt(request);
            ChatResponse response = model.call(prompt);
            return convertResponse(response, request.getModel());
        } catch (Exception e) {
            log.error("Qwen 调用失败", e);
            throw new RuntimeException("Qwen 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> chatStream(LLMRequest request) {
        try {
            DashScopeChatModel model = getChatModel();
            Prompt prompt = buildPrompt(request);
            return model.stream(prompt)
                    .map(response -> response.getResults().get(0).getOutput().getText());
        } catch (Exception e) {
            log.error("Qwen 流式调用失败", e);
            return Flux.error(new RuntimeException("Qwen 流式调用失败: " + e.getMessage(), e));
        }
    }

    @Override
    public String getProviderName() {
        return "alibaba";
    }

    @Override
    public boolean supports(String model) {
        if (model == null) return false;
        String lower = model.toLowerCase();
        // 通义千问系列
        return lower.contains("qwen") ||
               // DeepSeek 通过阿里云百炼
               lower.contains("deepseek") ||
               // 默认支持（当未指定模型时）
               model.equals("alibaba") ||
               model.equals("qwen-plus") ||
               model.equals("qwen-turbo") ||
               model.equals("qwen-max") ||
               model.equals("qwen-long");
    }

    private synchronized DashScopeChatModel getChatModel() {
        if (chatModel == null) {
            LLMConfiguration.AlibabaConfig config = configuration.getAlibaba();
            DashScopeChatOptions options = DashScopeChatOptions.builder()
                    .withModel(config.getModel())
                    .withTemperature(config.getTemperature())
                    .withMaxToken(config.getMaxTokens())
                    .build();
            com.alibaba.cloud.ai.dashscope.api.DashScopeApi api =
                    new com.alibaba.cloud.ai.dashscope.api.DashScopeApi.Builder()
                            .apiKey(config.getApiKey())
                            .baseUrl(config.getBaseUrl())
                            .build();
            chatModel = DashScopeChatModel.builder()
                    .dashScopeApi(api)
                    .defaultOptions(options)
                    .build();
        }
        return chatModel;
    }

    private Prompt buildPrompt(LLMRequest request) {
        List<org.springframework.ai.chat.messages.Message> messages = request.getMessages().stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());

        LLMConfiguration.AlibabaConfig config = configuration.getAlibaba();

        Map<String, Object> optionsMap = new HashMap<>();
        optionsMap.put("model", request.getModel() != null ? request.getModel() : config.getModel());
        if (request.getTemperature() != null) {
            optionsMap.put("temperature", request.getTemperature());
        } else {
            optionsMap.put("temperature", config.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            optionsMap.put("max_tokens", request.getMaxTokens());
        } else {
            optionsMap.put("max_tokens", config.getMaxTokens());
        }

        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withModel((String) optionsMap.get("model"))
                .withTemperature((Double) optionsMap.get("temperature"))
                .withMaxToken((Integer) optionsMap.get("max_tokens"))
                .build();

        return new Prompt(messages, (org.springframework.ai.chat.prompt.ChatOptions) options);
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
            model = requestedModel != null ? requestedModel : configuration.getAlibaba().getModel();
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
