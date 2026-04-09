package com.exceptioncoder.llm.infrastructure.config;

import com.exceptioncoder.llm.domain.model.AgentDefinition;
import com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository;
import com.exceptioncoder.llm.domain.registry.ToolRegistry;
import com.exceptioncoder.llm.infrastructure.agent.annotation.AgentGroup;
import com.exceptioncoder.llm.infrastructure.agent.annotation.AgentGroupProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 秘书 Agent 初始化器
 * 服务启动后自动注册系统级秘书 Agent 定义
 */
@Slf4j
@Component
@AgentGroup(
        id = "secretary",
        name = "个人秘书智能体",
        description = "智能个人秘书，支持日程管理、待办管理、笔记检索等功能"
)
public class SecretaryAgentInitializer implements ApplicationListener<ApplicationReadyEvent>, AgentGroupProvider {

    private static final String SECRETARY_AGENT_ID = "secretary-default";

    private final AgentDefinitionRepository agentRepository;
    private final ToolRegistry toolRegistry;

    public SecretaryAgentInitializer(AgentDefinitionRepository agentRepository, ToolRegistry toolRegistry) {
        this.agentRepository = agentRepository;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (agentRepository.existsById(SECRETARY_AGENT_ID)) {
            log.info("秘书 Agent 已存在，跳过初始化");
            return;
        }

        // 收集已注册的秘书工具
        List<String> secretaryToolIds = toolRegistry.getAllTools().stream()
                .map(t -> t.id())
                .filter(id -> isSecretaryTool(id))
                .toList();

        String systemPrompt = buildSystemPrompt();

        AgentDefinition secretary = AgentDefinition.builder()
                .id(SECRETARY_AGENT_ID)
                .name("个人秘书")
                .description("智能个人秘书，支持日程管理、待办管理、笔记检索等功能")
                .systemPrompt(systemPrompt)
                .toolIds(secretaryToolIds)
                .maxIterations(10)
                .timeoutSeconds(120)
                .enabled(true)
                .build();

        agentRepository.save(secretary);
        log.info("秘书 Agent 初始化完成，工具数量: {}", secretaryToolIds.size());
    }

    @Override
    public List<String> getAgentIds() {
        return List.of(SECRETARY_AGENT_ID);
    }

    private boolean isSecretaryTool(String toolId) {
        return toolId.startsWith("schedule_") ||
               toolId.startsWith("todo_") ||
               toolId.startsWith("note_");
    }

    private String buildSystemPrompt() {
        return """
                你是一个智能个人秘书，擅长帮助用户管理日程、待办事项、记录笔记和检索信息。

                你的职责：
                1. 日程管理：帮助用户添加、查看、完成日程安排
                2. 待办管理：帮助用户创建、跟踪、完成待办事项
                3. 笔记检索：帮助用户搜索已有的碎片记录和笔记
                4. 信息整理：为用户提供清晰、有条理的信息汇总

                工作原则：
                - 如果用户提到时间相关的事项，主动询问是否需要添加日程
                - 如果用户提到需要做某事，主动询问是否需要添加待办
                - 回复简洁明了，重要信息用列表格式展示
                - 当上下文包含记忆信息时，结合历史偏好来回复
                """;
    }
}
