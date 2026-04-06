package com.exceptioncoder.llm.application.devplan.node;

import com.exceptioncoder.llm.domain.devplan.model.AgentOutput;
import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.model.ImpactAnalysis;
import com.exceptioncoder.llm.domain.devplan.model.RequirementIntent;
import com.exceptioncoder.llm.domain.devplan.service.DevPlanAgentRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 需求分析节点（AnalyzeNode）—— 开发方案生成流程的第二个执行节点。
 *
 * <p>本类属于 <b>应用层（Application Layer）</b>，是 devplan 模块 StateGraph 的 Node 2。
 * 职责是调用 {@link AgentRole#REQUIREMENT_ANALYZER} Agent 对用户需求进行深度分析，
 * 提取需求意图（{@link RequirementIntent}）和影响面分析（{@link ImpactAnalysis}），
 * 并将结果写入不可变的 {@link DevPlanState} 传递给下游节点。</p>
 *
 * <h3>设计思路</h3>
 * <ul>
 *   <li>在 ScanNode 获取到项目结构之后执行，利用上下文中的结构信息辅助需求理解</li>
 *   <li>通过 {@link DevPlanAgentRouter} 路由到具体的 Agent 实现，保持节点与 Agent 解耦</li>
 *   <li>记录节点执行耗时和 Token 使用量，供下游统计和监控使用</li>
 * </ul>
 *
 * <h3>协作关系</h3>
 * <ul>
 *   <li>{@link DevPlanAgentRouter} — Agent 路由器，将请求分发到 REQUIREMENT_ANALYZER Agent</li>
 *   <li>{@link DevPlanFlowDefinition} — 流程编排器，在 ScanNode 之后调用本节点</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Component
public class AnalyzeNode {

    /** Agent 路由器 —— 将请求分发到对应角色的 Agent 实现 */
    private final DevPlanAgentRouter agentRouter;

    /**
     * 构造 AnalyzeNode 实例。
     *
     * @param agentRouter Agent 路由器，用于将请求分发到 REQUIREMENT_ANALYZER Agent
     */
    public AnalyzeNode(DevPlanAgentRouter agentRouter) {
        this.agentRouter = agentRouter;
    }

    /**
     * 执行需求分析。
     *
     * <p>处理步骤：</p>
     * <ol>
     *   <li>调用 REQUIREMENT_ANALYZER Agent 分析用户需求</li>
     *   <li>从 Agent 输出中提取 {@link RequirementIntent} 和 {@link ImpactAnalysis}</li>
     *   <li>记录节点执行耗时和 Token 使用量</li>
     *   <li>构建新的 {@link DevPlanState} 并返回，状态标记为 {@code ANALYZING_COMPLETE}</li>
     * </ol>
     *
     * @param state 上游传入的流程状态，需包含 ScanNode 产出的项目结构与架构拓扑
     * @return 包含需求意图和影响面分析的新状态
     */
    public DevPlanState execute(DevPlanState state) {
        log.info("执行 AnalyzeNode，taskId={}", state.taskId());
        long start = System.currentTimeMillis();

        // 1. 调用 REQUIREMENT_ANALYZER Agent 深度分析用户需求
        AgentOutput output = agentRouter.route(AgentRole.REQUIREMENT_ANALYZER, state);

        // 2. 记录节点执行耗时和 Token 使用量
        long elapsed = System.currentTimeMillis() - start;
        var timings = new java.util.HashMap<>(state.nodeTimings());
        timings.put("analyze", elapsed);
        var tokenUsage = new java.util.HashMap<>(state.agentTokenUsage());
        tokenUsage.put(AgentRole.REQUIREMENT_ANALYZER.name(), output.tokenUsage());

        // 3. 从 Agent 结构化输出中提取需求意图和影响面分析
        RequirementIntent intent = (RequirementIntent) output.structuredData().get("intent");
        ImpactAnalysis impact = (ImpactAnalysis) output.structuredData().get("impact");

        // 4. 构建新的不可变 State，携带本节点产出传递给下游
        return DevPlanState.builder()
                .taskId(state.taskId())
                .projectPath(state.projectPath())
                .requirement(state.requirement())
                .structure(state.structure())
                .topology(state.topology())
                .intent(intent)
                .impact(impact)
                .status("ANALYZING_COMPLETE")
                .nodeTimings(timings)
                .agentTokenUsage(tokenUsage)
                .correctionCount(state.correctionCount())
                .reviewIssues(state.reviewIssues())
                .build();
    }
}
