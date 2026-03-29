package com.exceptioncoder.llm.application.service;

import com.exceptioncoder.llm.domain.executor.AgentExecutor;
import com.exceptioncoder.llm.domain.model.*;
import com.exceptioncoder.llm.domain.repository.SecretaryMemoryRepository;
import com.exceptioncoder.llm.domain.repository.SecretaryScheduleRepository;
import com.exceptioncoder.llm.domain.repository.SecretaryTodoRepository;
import com.exceptioncoder.llm.domain.registry.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 个人秘书服务
 * 组装长期记忆注入 systemPrompt，调用 AgentExecutor 执行，执行后更新记忆
 */
@Slf4j
@Service
public class SecretaryService {

    private static final String SECRETARY_AGENT_ID = "secretary-default";
    private static final String DEFAULT_USER = "default";

    private final AgentExecutor agentExecutor;
    private final SecretaryMemoryRepository memoryRepository;
    private final SecretaryScheduleRepository scheduleRepository;
    private final SecretaryTodoRepository todoRepository;
    private final ToolRegistry toolRegistry;

    public SecretaryService(
            AgentExecutor agentExecutor,
            SecretaryMemoryRepository memoryRepository,
            SecretaryScheduleRepository scheduleRepository,
            SecretaryTodoRepository todoRepository,
            ToolRegistry toolRegistry
    ) {
        this.agentExecutor = agentExecutor;
        this.memoryRepository = memoryRepository;
        this.scheduleRepository = scheduleRepository;
        this.todoRepository = todoRepository;
        this.toolRegistry = toolRegistry;
    }

    /**
     * 执行秘书对话
     */
    public AgentExecutionResult chat(String userInput, String sessionId) {
        String memoryContext = buildMemoryContext(DEFAULT_USER);
        Map<String, Object> context = Map.of(
                "sessionId", sessionId != null ? sessionId : UUID.randomUUID().toString(),
                "memoryContext", memoryContext
        );
        var request = AgentExecutor.AgentExecutionRequest.builder()
                .executionId(UUID.randomUUID().toString())
                .agentId(SECRETARY_AGENT_ID)
                .userInput(userInput)
                .context(context)
                .build();
        log.info("秘书对话: sessionId={}, input={}", sessionId, userInput);
        return agentExecutor.execute(request);
    }

    /**
     * 获取长期记忆列表
     */
    public List<SecretaryMemory> getMemory() {
        return memoryRepository.findByUserId(DEFAULT_USER);
    }

    /**
     * 清除长期记忆
     */
    public void clearMemory() {
        memoryRepository.deleteByUserId(DEFAULT_USER);
        log.info("已清除秘书长期记忆: userId={}", DEFAULT_USER);
    }

    /**
     * 保存一条记忆
     */
    public SecretaryMemory saveMemory(SecretaryMemory.MemoryType type, String content) {
        SecretaryMemory memory = new SecretaryMemory(null, DEFAULT_USER, type, content, null, null);
        return memoryRepository.save(memory);
    }

    /**
     * 获取已注册工具列表
     */
    public List<ToolDefinition> getTools() {
        return toolRegistry.getAllTools();
    }

    /**
     * 构建记忆上下文字符串，注入到 systemPrompt
     */
    private String buildMemoryContext(String userId) {
        List<SecretaryMemory> memories = memoryRepository.findByUserId(userId);
        if (memories.isEmpty()) return "";
        return memories.stream()
                .map(m -> "[" + m.type().name() + "] " + m.content())
                .collect(Collectors.joining("\n"));
    }
}
