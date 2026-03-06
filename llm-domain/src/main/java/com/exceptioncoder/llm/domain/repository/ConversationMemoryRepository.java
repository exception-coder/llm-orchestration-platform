package com.exceptioncoder.llm.domain.repository;

import com.exceptioncoder.llm.domain.model.Message;

import java.util.List;

/**
 * 对话记忆仓储接口
 * 用于管理对话上下文
 */
public interface ConversationMemoryRepository {
    
    /**
     * 保存消息
     */
    void saveMessage(String conversationId, Message message);
    
    /**
     * 获取对话历史
     */
    List<Message> getHistory(String conversationId);
    
    /**
     * 获取最近N条消息
     */
    List<Message> getRecentMessages(String conversationId, int limit);
    
    /**
     * 清空对话历史
     */
    void clearHistory(String conversationId);
}

