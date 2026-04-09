package com.exceptioncoder.llm.infrastructure.agent.annotation;

import com.exceptioncoder.llm.domain.model.GraphDefinition;
import com.exceptioncoder.llm.domain.model.GraphEdge;
import com.exceptioncoder.llm.domain.model.GraphNode;
import com.exceptioncoder.llm.domain.model.NodeType;
import com.exceptioncoder.llm.domain.repository.GraphDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 启动时扫描所有带 {@link AgentGroup} 注解的 Bean，
 * 自动创建对应的 GraphDefinition（智能体）并将其 Agent 成员注册为 Graph 节点。
 *
 * <p>执行顺序 {@code @Order(200)}，确保在所有 AgentInitializer（Order=100）之后运行，
 * 此时 Agent 已经全部注册到数据库。
 *
 * @author zhangkai
 * @since 2026-04-09
 */
@Slf4j
@Component
@Order(200)
public class AgentGroupScanner implements ApplicationListener<ApplicationReadyEvent> {

    private final GraphDefinitionRepository graphRepository;

    public AgentGroupScanner(GraphDefinitionRepository graphRepository) {
        this.graphRepository = graphRepository;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        var context = event.getApplicationContext();
        Map<String, Object> beans = context.getBeansWithAnnotation(AgentGroup.class);

        int created = 0;
        int skipped = 0;

        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object bean = entry.getValue();
            AgentGroup annotation = bean.getClass().getAnnotation(AgentGroup.class);
            if (annotation == null) continue;

            String graphId = annotation.id();

            if (graphRepository.existsById(graphId)) {
                log.info("智能体 Graph 已存在，跳过: graphId={}", graphId);
                skipped++;
                continue;
            }

            // 获取 Agent 成员列表
            List<String> agentIds = resolveAgentIds(bean);
            if (agentIds.isEmpty()) {
                log.warn("智能体 {} 无 Agent 成员，跳过 Graph 创建", graphId);
                skipped++;
                continue;
            }

            // 构建节点和边（串行编排）
            List<GraphNode> nodes = new ArrayList<>();
            List<GraphEdge> edges = new ArrayList<>();

            for (int i = 0; i < agentIds.size(); i++) {
                String agentId = agentIds.get(i);
                nodes.add(new GraphNode(
                        agentId,
                        NodeType.LLM,
                        agentId,
                        Map.of("agentId", agentId)
                ));

                if (i > 0) {
                    edges.add(new GraphEdge(agentIds.get(i - 1), agentId, null));
                }
            }

            String entryNodeId = agentIds.get(0);

            GraphDefinition graph = new GraphDefinition(
                    graphId,
                    annotation.name(),
                    annotation.description(),
                    nodes,
                    edges,
                    entryNodeId
            );

            graphRepository.save(graph);
            log.info("智能体 Graph 注册完成: graphId={}, name={}, agents={}",
                    graphId, annotation.name(), agentIds);
            created++;
        }

        log.info("智能体 Graph 扫描汇总: 创建={}, 跳过={}", created, skipped);
    }

    private List<String> resolveAgentIds(Object bean) {
        if (bean instanceof AgentGroupProvider provider) {
            return provider.getAgentIds();
        }
        log.warn("Bean {} 带有 @AgentGroup 但未实现 AgentGroupProvider，无法获取 Agent 成员",
                bean.getClass().getSimpleName());
        return List.of();
    }
}
