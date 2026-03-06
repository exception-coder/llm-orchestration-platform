package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.application.service.ConversationService;
import com.exceptioncoder.llm.application.service.LLMOrchestrationService;
import com.exceptioncoder.llm.domain.model.LLMRequest;
import com.exceptioncoder.llm.domain.model.LLMResponse;
import com.exceptioncoder.llm.domain.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 对话用例
 * 编排对话流程：获取历史 -> 调用LLM -> 保存结果
 */
@Slf4j
@Component
public class ChatUseCase {
    
    private final LLMOrchestrationService orchestrationService;
    private final ConversationService conversationService;
    
    public ChatUseCase(LLMOrchestrationService orchestrationService,
                       ConversationService conversationService) {
        this.orchestrationService = orchestrationService;
        this.conversationService = conversationService;
    }
    
    /**
     * 执行对话
     */
    public LLMResponse execute(String conversationId, LLMRequest request) {
        // 1. 获取对话历史
        List<Message> history = conversationService.getRecentMessages(conversationId, 10);
        
        // 2. 构建完整的消息列表
        List<Message> messages = new ArrayList<>(history);
        if (request.getPrompt() != null) {
            messages.add(Message.builder()
                    .role("user")
                    .content(request.getPrompt())
                    .build());
        }
        request.setMessages(messages);
        
        // 3. 调用LLM
        LLMResponse response = orchestrationService.chat(request);
        
        // 4. 保存对话历史
        conversationService.addMessage(conversationId, 
                Message.builder().role("user").content(request.getPrompt()).build());
        conversationService.addMessage(conversationId,
                Message.builder().role("assistant").content(response.getContent()).build());
        
        log.info("对话 {} 完成，使用模型: {}", conversationId, response.getModel());
        return response;
    }
    
    /**
     * 执行流式对话
     */
    public Flux<String> executeStream(String conversationId, LLMRequest request) {
        // 1. 获取对话历史
        List<Message> history = conversationService.getRecentMessages(conversationId, 10);
        
        // 2. 构建完整的消息列表
        List<Message> messages = new ArrayList<>(history);
        if (request.getPrompt() != null) {
            messages.add(Message.builder()
                    .role("user")
                    .content(request.getPrompt())
                    .build());
        }
        request.setMessages(messages);
        
        // 3. 保存用户消息
        conversationService.addMessage(conversationId, 
                Message.builder().role("user").content(request.getPrompt()).build());
        
        // 4. 调用LLM流式接口
        AtomicReference<StringBuilder> fullContent = new AtomicReference<>(new StringBuilder());
        
        return orchestrationService.chatStream(request)
                .doOnNext(content -> fullContent.get().append(content))
                .doOnComplete(() -> {
                    // 5. 流式完成后保存完整的助手回复
                    conversationService.addMessage(conversationId,
                            Message.builder().role("assistant").content(fullContent.get().toString()).build());
                    log.info("流式对话 {} 完成", conversationId);
                })
                .doOnError(error -> log.error("流式对话 {} 失败", conversationId, error));
    }
}

