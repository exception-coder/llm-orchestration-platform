package com.exceptioncoder.llm.application.devplan.node;

import com.exceptioncoder.llm.domain.devplan.model.AgentOutput;
import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.devplan.model.ArchTopology;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.model.ProjectStructure;
import com.exceptioncoder.llm.domain.devplan.service.DevPlanAgentRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 代码感知节点（ScanNode）—— 开发方案生成流程的第一个执行节点。
 *
 * <p>本类属于 <b>应用层（Application Layer）</b>，是 devplan 模块 StateGraph 的 Node 1。
 * 职责是调用 {@link AgentRole#CODE_AWARENESS} Agent 对目标项目进行代码扫描，
 * 提取项目结构（{@link ProjectStructure}）和架构拓扑（{@link ArchTopology}），
 * 并将结果写入不可变的 {@link DevPlanState} 传递给下游节点。</p>
 *
 * <h3>设计思路</h3>
 * <ul>
 *   <li>每个 Node 只负责调用对应 Agent 并将结果写入 State，保持编排逻辑的简洁性</li>
 *   <li>通过 {@link DevPlanAgentRouter} 路由到具体的 Agent 实现，实现节点与 Agent 的解耦</li>
 *   <li>记录节点执行耗时和 Token 使用量，供下游统计和监控使用</li>
 * </ul>
 *
 * <h3>协作关系</h3>
 * <ul>
 *   <li>{@link DevPlanAgentRouter} — Agent 路由器，将请求分发到 CODE_AWARENESS Agent</li>
 *   <li>{@link DevPlanFlowDefinition} — 流程编排器，按顺序调用本节点</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Component
public class ScanNode {

    /** Agent 路由器 —— 将请求分发到对应角色的 Agent 实现 */
    private final DevPlanAgentRouter agentRouter;

    /**
     * 构造 ScanNode 实例。
     *
     * @param agentRouter Agent 路由器，用于将请求分发到 CODE_AWARENESS Agent
     */
    public ScanNode(DevPlanAgentRouter agentRouter) {
        this.agentRouter = agentRouter;
    }

    /**
     * 执行代码感知扫描。
     *
     * <p>处理步骤：</p>
     * <ol>
     *   <li>调用 CODE_AWARENESS Agent 扫描目标项目</li>
     *   <li>从 Agent 输出中提取 {@link ProjectStructure} 和 {@link ArchTopology}</li>
     *   <li>记录节点执行耗时和 Token 使用量</li>
     *   <li>构建新的 {@link DevPlanState} 并返回，状态标记为 {@code SCANNING_COMPLETE}</li>
     * </ol>
     *
     * @param state 上游传入的流程状态，需包含 projectPath 等基础信息
     * @return 包含项目结构和架构拓扑的新状态
     */
    public DevPlanState execute(DevPlanState state) {
        log.info("执行 ScanNode，projectPath={}", state.projectPath());
        long start = System.currentTimeMillis();

        // 1. 调用 CODE_AWARENESS Agent 扫描目标项目的代码结构和架构拓扑
        AgentOutput output = agentRouter.route(AgentRole.CODE_AWARENESS, state);

        // 2. 记录节点执行耗时和 Token 使用量
        long elapsed = System.currentTimeMillis() - start;
        var timings = new java.util.HashMap<>(state.nodeTimings());
        timings.put("scan", elapsed);
        var tokenUsage = new java.util.HashMap<>(state.agentTokenUsage());
        tokenUsage.put(AgentRole.CODE_AWARENESS.name(), output.tokenUsage());

        // 3. 从 Agent 结构化输出中提取项目结构和架构拓扑
        ProjectStructure structure = (ProjectStructure) output.structuredData().get("structure");
        ArchTopology topology = (ArchTopology) output.structuredData().get("topology");

        // 4. 构建新的不可变 State，携带本节点产出传递给下游
        return DevPlanState.builder()
                .taskId(state.taskId())
                .projectPath(state.projectPath())
                .requirement(state.requirement())
                .structure(structure)
                .topology(topology)
                .status("SCANNING_COMPLETE")
                .nodeTimings(timings)
                .agentTokenUsage(tokenUsage)
                .correctionCount(state.correctionCount())
                .reviewIssues(state.reviewIssues())
                .build();
    }
}
