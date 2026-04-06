package com.exceptioncoder.llm.domain.devplan.service;

import com.exceptioncoder.llm.domain.devplan.model.AgentOutput;
import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;

/**
 * Agent 路由接口 — 按角色分发执行
 */
public interface DevPlanAgentRouter {

    /**
     * 将任务分发给指定角色的 Agent 执行
     *
     * @param role  Agent 角色
     * @param state 当前全局状态
     * @return Agent 执行输出
     */
    AgentOutput route(AgentRole role, DevPlanState state);
}
