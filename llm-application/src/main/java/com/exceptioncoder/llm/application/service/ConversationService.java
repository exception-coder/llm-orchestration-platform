package com.exceptioncoder.llm.application.service;

import com.exceptioncoder.llm.domain.model.Message;
import com.exceptioncoder.llm.domain.repository.ConversationMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 对话管理服务
 * 负责管理对话上下文和历史
 */
@Slf4j
@Service
public class ConversationService {
    
    private final ConversationMemoryRepository memoryRepository;
    
    public ConversationService(ConversationMemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }
    
    /**
     * 添加消息到对话历史
     */
    public void addMessage(String conversationId, Message message) {
        memoryRepository.saveMessage(conversationId, message);
        log.debug("已保存消息到对话 {}", conversationId);
    }
    
    /**
     * 获取对话历史
     */
    public List<Message> getHistory(String conversationId) {
        return memoryRepository.getHistory(conversationId);
    }
    
    /**
     * 获取最近的消息
     */
    public List<Message> getRecentMessages(String conversationId, int limit) {
        return memoryRepository.getRecentMessages(conversationId, limit);
    }
    
    /**
     * 清空对话
     */
    public void clearConversation(String conversationId) {
        memoryRepository.clearHistory(conversationId);
        log.info("已清空对话 {}", conversationId);
    }
}

