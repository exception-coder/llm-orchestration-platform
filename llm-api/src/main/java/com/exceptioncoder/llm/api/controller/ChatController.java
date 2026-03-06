package com.exceptioncoder.llm.api.controller;

import com.exceptioncoder.llm.api.dto.ChatRequest;
import com.exceptioncoder.llm.api.dto.ChatResponse;
import com.exceptioncoder.llm.application.usecase.ChatUseCase;
import com.exceptioncoder.llm.domain.model.LLMRequest;
import com.exceptioncoder.llm.domain.model.LLMResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 对话 API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    
    private final ChatUseCase chatUseCase;
    private final ObjectMapper objectMapper;
    
    public ChatController(ChatUseCase chatUseCase, ObjectMapper objectMapper) {
        this.chatUseCase = chatUseCase;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 发送对话消息（非流式）
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatRequest request) {
        log.info("收到非流式对话请求: conversationId={}, provider={}, model={}", 
                request.getConversationId(), request.getProvider(), request.getModel());
        
        return chatNonStream(request);
    }
    
    /**
     * 发送对话消息（流式）
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        log.info("收到流式对话请求: conversationId={}, provider={}, model={}", 
                request.getConversationId(), request.getProvider(), request.getModel());
        
        return streamChat(request);
    }
    
    /**
     * 流式对话
     */
    private SseEmitter streamChat(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        
        LLMRequest llmRequest = LLMRequest.builder()
                .prompt(request.getMessage())
                .provider(request.getProvider())
                .model(request.getModel())
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .stream(true)
                .build();
        
        // 异步处理流式响应
        new Thread(() -> {
            try {
                Flux<String> contentStream = chatUseCase.executeStream(request.getConversationId(), llmRequest);
                
                contentStream.subscribe(
                    content -> {
                        try {
                            // 发送内容片段
                            Map<String, Object> data = new HashMap<>();
                            data.put("content", content);
                            String jsonData = objectMapper.writeValueAsString(data);
                            log.debug("发送流式数据: {}", jsonData);
                            emitter.send(SseEmitter.event().data(jsonData));
                        } catch (IOException e) {
                            log.error("发送流式数据失败", e);
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        log.error("流式处理失败", error);
                        emitter.completeWithError(error);
                    },
                    () -> {
                        try {
                            // 发送完成标记
                            log.debug("发送完成标记");
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("发送完成标记失败", e);
                            emitter.completeWithError(e);
                        }
                    }
                );
            } catch (Exception e) {
                log.error("流式对话处理异常", e);
                emitter.completeWithError(e);
            }
        }).start();
        
        return emitter;
    }
    
    /**
     * 非流式对话
     */
    private SseEmitter chatNonStream(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(60000L);
        
        LLMRequest llmRequest = LLMRequest.builder()
                .prompt(request.getMessage())
                .provider(request.getProvider())
                .model(request.getModel())
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .stream(false)
                .build();
        
        new Thread(() -> {
            try {
                LLMResponse response = chatUseCase.execute(request.getConversationId(), llmRequest);
                
                // 发送完整响应
                Map<String, Object> data = new HashMap<>();
                data.put("content", response.getContent());
                data.put("tokenUsage", response.getTokenUsage());
                
                String jsonData = objectMapper.writeValueAsString(data);
                log.debug("发送非流式数据: {}", jsonData);
                emitter.send(SseEmitter.event().data(jsonData));
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                log.error("非流式对话处理异常", e);
                emitter.completeWithError(e);
            }
        }).start();
        
        return emitter;
    }
}

