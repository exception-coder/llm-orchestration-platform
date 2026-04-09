package com.exceptioncoder.llm.infrastructure.agent.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 声明智能体分组内的单个 Agent 成员，用于 {@link AgentGroup#members()} 无法覆盖的场景。
 * 由 {@link AgentGroupProvider} 接口动态提供成员列表。
 *
 * @author zhangkai
 * @since 2026-04-09
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentGroupMember {

    /**
     * Agent ID（必须与 AgentDefinitionRepository 中的 ID 一致）
     */
    String agentId();

    /**
     * 节点显示名称（缺省时使用 agentId）
     */
    String nodeName() default "";

    /**
     * 在编排中的顺序（值越小越靠前）
     */
    int order() default 0;
}
