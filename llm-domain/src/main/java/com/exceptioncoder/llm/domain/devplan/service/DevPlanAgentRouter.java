package com.exceptioncoder.llm.domain.devplan.service;

import com.exceptioncoder.llm.domain.devplan.model.AgentOutput;
import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;

/**
 * Agent 路由接口 -- 按角色分发任务到对应 Agent 执行。
 *
 * <p>属于 Domain 层 devplan 模块。作为 Graph 节点与具体 Agent 实现之间的
 * 解耦桥梁：节点只需声明需要哪个角色，路由器负责查找并调用对应的 Agent。
 * 实现类通常维护一个 {@link AgentRole} → Agent 实例的映射表。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public interface DevPlanAgentRouter {

    /**
     * 将任务分发给指定角色的 Agent 执行。
     *
     * @param role  目标 Agent 角色，决定由哪个 Agent 处理
     * @param state 当前全局状态快照，Agent 从中读取所需的上游数据
     * @return Agent 执行输出，包含原始文本、结构化数据及可观测性指标
     * @throws IllegalArgumentException 若指定角色未注册对应的 Agent 实现
     */
    AgentOutput route(AgentRole role, DevPlanState state);
}
