package com.exceptioncoder.llm.infrastructure.agent.annotation;

import java.util.List;

/**
 * 由带 {@link AgentGroup} 注解的 Bean 实现，动态提供该智能体分组下的 Agent 成员列表。
 *
 * <p>Scanner 在注册 Graph 时调用此方法获取成员 Agent ID，
 * 按返回顺序依次创建 Graph 节点和边（串行编排）。
 *
 * @author zhangkai
 * @since 2026-04-09
 */
public interface AgentGroupProvider {

    /**
     * 返回该智能体分组下所有 Agent ID（有序）。
     */
    List<String> getAgentIds();
}
