package com.exceptioncoder.llm.infrastructure.devplan.config;

import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.model.AgentDefinition;
import com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository;
import com.exceptioncoder.llm.infrastructure.devplan.agent.DevPlanAgentConfig;
import com.exceptioncoder.llm.infrastructure.devplan.tool.DevPlanToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DevPlan Agent 初始化器 -- 应用启动时将 4 个角色 Agent 写入数据库。
 *
 * <p>监听 {@link ApplicationReadyEvent}，确保在 ToolScanner 之后执行。
 * 幂等设计：existsById 检查，已存在的 Agent 不会被覆盖。
 *
 * <p>参照 {@link com.exceptioncoder.llm.infrastructure.config.SecretaryAgentInitializer} 实现。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
@Order(100) // 确保在 ToolScanner（默认 order）之后执行
public class DevPlanAgentInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final AgentDefinitionRepository agentRepository;
    private final DevPlanAgentConfig agentConfig;

    private static final Map<AgentRole, String> AGENT_NAMES = Map.of(
            AgentRole.CODE_AWARENESS, "代码感知分析专家",
            AgentRole.REQUIREMENT_ANALYZER, "需求分析专家",
            AgentRole.SOLUTION_ARCHITECT, "方案架构师",
            AgentRole.PLAN_REVIEWER, "方案审查专家"
    );

    private static final Map<AgentRole, String> AGENT_DESCRIPTIONS = Map.of(
            AgentRole.CODE_AWARENESS, "扫描项目结构、解析依赖、分析代码结构、提取配置、建立向量索引",
            AgentRole.REQUIREMENT_ANALYZER, "分析需求影响范围、通过代码搜索定位涉及的现有类和依赖链",
            AgentRole.SOLUTION_ARCHITECT, "基于项目画像和影响分析，按模板生成符合架构规范的设计文档",
            AgentRole.PLAN_REVIEWER, "对设计文档进行完整性、一致性、可行性、规范性四维度质量评审"
    );

    public DevPlanAgentInitializer(AgentDefinitionRepository agentRepository,
                                    DevPlanAgentConfig agentConfig) {
        this.agentRepository = agentRepository;
        this.agentConfig = agentConfig;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        int created = 0;
        int skipped = 0;

        for (AgentRole role : AgentRole.values()) {
            String agentId = agentConfig.getAgentId(role);

            if (agentRepository.existsById(agentId)) {
                log.info("DevPlan Agent 已存在，跳过: agentId={}, role={}", agentId, role);
                skipped++;
                continue;
            }

            List<String> toolIds = DevPlanToolRegistry.ROLE_TOOL_MAPPING.getOrDefault(role, List.of());

            AgentDefinition agent = AgentDefinition.builder()
                    .id(agentId)
                    .name(AGENT_NAMES.get(role))
                    .description(AGENT_DESCRIPTIONS.get(role))
                    .systemPrompt(agentConfig.getSystemPrompt(role))
                    .toolIds(toolIds)
                    .maxIterations(10)
                    .timeoutSeconds(120)
                    .enabled(true)
                    .build();

            agentRepository.save(agent);
            log.info("DevPlan Agent 初始化完成: agentId={}, role={}, toolIds={}",
                    agentId, role, toolIds);
            created++;
        }

        log.info("DevPlan Agent 初始化汇总: 创建={}, 跳过={}", created, skipped);
    }
}
