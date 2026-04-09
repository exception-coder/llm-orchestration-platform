package com.exceptioncoder.llm.infrastructure.provider;

import com.exceptioncoder.llm.domain.exception.RateLimitExceededException;
import com.exceptioncoder.llm.domain.model.LLMRequest;
import com.exceptioncoder.llm.domain.model.LLMResponse;
import com.exceptioncoder.llm.domain.model.Message;
import com.exceptioncoder.llm.domain.model.TokenUsage;
import com.exceptioncoder.llm.domain.service.LLMProvider;
import com.exceptioncoder.llm.infrastructure.config.LLMConfiguration;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 智谱 AI Provider 实现
 * 使用 OpenAI 兼容协议（Spring AI OpenAI）
 */
@Slf4j
@Component
public class ZhipuProvider implements LLMProvider {

    private final LLMConfiguration configuration;
    private OpenAiChatModel chatModel;
    private RateLimiter rateLimiter;

    public ZhipuProvider(LLMConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public LLMResponse chat(LLMRequest request) {
        acquirePermit();
        try {
            OpenAiChatModel model = getOrCreateChatModel();
            Prompt prompt = buildPrompt(request);
            ChatResponse response = model.call(prompt);
            return convertResponse(response, request.getModel());
        } catch (NonTransientAiException e) {
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                throw new RateLimitExceededException(getProviderName());
            }
            log.error("智谱 AI 调用失败", e);
            throw new RuntimeException("智谱 AI 调用失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("智谱 AI 调用失败", e);
            throw new RuntimeException("智谱 AI 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> chatStream(LLMRequest request) {
        acquirePermit();
        try {
            OpenAiChatModel model = getOrCreateChatModel();
            Prompt prompt = buildPrompt(request);
            return model.stream(prompt)
                    .map(response -> response.getResults().get(0).getOutput().getText())
                    .filter(text -> text != null);
        } catch (Exception e) {
            log.error("智谱 AI 流式调用失败", e);
            return Flux.error(new RuntimeException("智谱 AI 流式调用失败: " + e.getMessage(), e));
        }
    }

    @Override
    public ChatModel getChatModel() {
        return getOrCreateChatModel();
    }

    @Override
    public String getProviderName() {
        return "zhipu";
    }

    @Override
    public boolean supports(String model) {
        if (model == null) return false;
        String lower = model.toLowerCase();
        return lower.contains("glm") ||
               lower.contains("zhipu") ||
               lower.equals("chatglm");
    }

    private synchronized OpenAiChatModel getOrCreateChatModel() {
        if (chatModel == null) {
            LLMConfiguration.ZhipuConfig config = configuration.getZhipu();
            OpenAiApi api = OpenAiApi.builder()
                    .apiKey(config.getApiKey())
                    .baseUrl(config.getBaseUrl())
                    .completionsPath("/v4/chat/completions")
                    .build();
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(config.getModel())
                    .temperature(config.getTemperature())
                    .maxTokens(config.getMaxTokens())
                    .build();
            chatModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(options)
                    .build();
            initRateLimiter();
        }
        return chatModel;
    }

    private void initRateLimiter() {
        int rpm = configuration.getZhipu().getRateLimit().getRpm();
        if (rpm > 0) {
            rateLimiter = RateLimiter.create(rpm / 60.0);
            log.info("智谱 AI 限速已启用: {} RPM", rpm);
        }
    }

    private void acquirePermit() {
        if (rateLimiter != null && !rateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
            throw new RateLimitExceededException(getProviderName());
        }
    }

    private Prompt buildPrompt(LLMRequest request) {
        List<org.springframework.ai.chat.messages.Message> messages = request.getMessages().stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());

        LLMConfiguration.ZhipuConfig config = configuration.getZhipu();
        String model = request.getModel() != null ? request.getModel() : config.getModel();
        Double temperature = request.getTemperature() != null ? request.getTemperature() : config.getTemperature();
        Integer maxTokens = request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
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
        var result = response.getResult();
        var metadata = response.getMetadata();

        String model = metadata.getModel();
        if (model == null || model.isEmpty()) {
            model = requestedModel != null ? requestedModel : configuration.getZhipu().getModel();
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
                .content(result.getOutput().getText())
                .model(model)
                .tokenUsage(tokenUsage)
                .finishReason(result.getMetadata().getFinishReason())
                .build();
    }
}
