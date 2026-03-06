package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.domain.model.Message;
import com.exceptioncoder.llm.domain.repository.ConversationMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存实现的对话仓储
 * 生产环境建议使用 Redis 或数据库
 */
@Slf4j
@Repository
public class InMemoryConversationRepository implements ConversationMemoryRepository {
    
    private final Map<String, List<Message>> conversationStore = new ConcurrentHashMap<>();
    
    @Override
    public void saveMessage(String conversationId, Message message) {
        conversationStore.computeIfAbsent(conversationId, k -> new ArrayList<>())
                .add(message);
    }
    
    @Override
    public List<Message> getHistory(String conversationId) {
        return new ArrayList<>(conversationStore.getOrDefault(conversationId, Collections.emptyList()));
    }
    
    @Override
    public List<Message> getRecentMessages(String conversationId, int limit) {
        List<Message> history = getHistory(conversationId);
        int size = history.size();
        if (size <= limit) {
            return history;
        }
        return history.subList(size - limit, size);
    }
    
    @Override
    public void clearHistory(String conversationId) {
        conversationStore.remove(conversationId);
    }
}

