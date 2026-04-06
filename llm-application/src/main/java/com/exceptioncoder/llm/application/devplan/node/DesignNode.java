package com.exceptioncoder.llm.application.devplan.node;

import com.exceptioncoder.llm.domain.devplan.model.AgentOutput;
import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanDocument;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.service.DevPlanAgentRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 方案生成节点（DesignNode）—— 开发方案生成流程的第三个执行节点。
 *
 * <p>本类属于 <b>应用层（Application Layer）</b>，是 devplan 模块 StateGraph 的 Node 3。
 * 职责是调用 {@link AgentRole#SOLUTION_ARCHITECT} Agent 基于已有的项目结构、
 * 需求意图和影响面，生成完整的开发方案文档（{@link DevPlanDocument}），
 * 并将结果写入不可变的 {@link DevPlanState} 传递给下游审查节点。</p>
 *
 * <h3>设计思路</h3>
 * <ul>
 *   <li>该节点可能在修正循环中被多次调用，每次调用时 State 中会携带上次审查的问题列表</li>
 *   <li>Agent 实现需根据 {@code reviewIssues} 和 {@code correctionCount} 进行针对性修正</li>
 *   <li>若 Agent 未返回结构化文档，则以原始输出文本兜底构建 {@link DevPlanDocument}</li>
 * </ul>
 *
 * <h3>协作关系</h3>
 * <ul>
 *   <li>{@link DevPlanAgentRouter} — Agent 路由器，将请求分发到 SOLUTION_ARCHITECT Agent</li>
 *   <li>{@link DevPlanFlowDefinition} — 流程编排器，在修正循环中调用本节点</li>
 *   <li>{@link ReviewNode} — 下游审查节点，对本节点生成的方案进行质量评审</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Component
public class DesignNode {

    /** Agent 路由器 —— 将请求分发到对应角色的 Agent 实现 */
    private final DevPlanAgentRouter agentRouter;

    /**
     * 构造 DesignNode 实例。
     *
     * @param agentRouter Agent 路由器，用于将请求分发到 SOLUTION_ARCHITECT Agent
     */
    public DesignNode(DevPlanAgentRouter agentRouter) {
        this.agentRouter = agentRouter;
    }

    /**
     * 执行方案生成。
     *
     * <p>处理步骤：</p>
     * <ol>
     *   <li>调用 SOLUTION_ARCHITECT Agent 生成开发方案</li>
     *   <li>从 Agent 输出中提取 {@link DevPlanDocument}，若为 null 则用原始文本兜底</li>
     *   <li>记录节点执行耗时和 Token 使用量</li>
     *   <li>构建新的 {@link DevPlanState} 并返回，状态标记为 {@code DESIGNING_COMPLETE}</li>
     * </ol>
     *
     * @param state 上游传入的流程状态，需包含需求意图、影响面分析；修正循环时还包含审查问题列表
     * @return 包含开发方案文档的新状态
     */
    public DevPlanState execute(DevPlanState state) {
        log.info("执行 DesignNode，taskId={}，correctionCount={}",
                state.taskId(), state.correctionCount());
        long start = System.currentTimeMillis();

        // 1. 调用 SOLUTION_ARCHITECT Agent 生成开发方案（修正循环时 State 中携带审查问题）
        AgentOutput output = agentRouter.route(AgentRole.SOLUTION_ARCHITECT, state);

        // 2. 记录节点执行耗时和 Token 使用量
        long elapsed = System.currentTimeMillis() - start;
        var timings = new java.util.HashMap<>(state.nodeTimings());
        timings.put("design", elapsed);
        var tokenUsage = new java.util.HashMap<>(state.agentTokenUsage());
        tokenUsage.put(AgentRole.SOLUTION_ARCHITECT.name(), output.tokenUsage());

        // 3. 提取方案文档，若 Agent 未返回结构化文档则以原始文本兜底
        DevPlanDocument document = (DevPlanDocument) output.structuredData().get("document");
        if (document == null) {
            document = new DevPlanDocument(output.rawOutput(), java.util.Map.of());
        }

        // 4. 构建新的不可变 State，携带方案文档传递给审查节点
        return DevPlanState.builder()
                .taskId(state.taskId())
                .projectPath(state.projectPath())
                .requirement(state.requirement())
                .structure(state.structure())
                .topology(state.topology())
                .intent(state.intent())
                .impact(state.impact())
                .document(document)
                .status("DESIGNING_COMPLETE")
                .nodeTimings(timings)
                .agentTokenUsage(tokenUsage)
                .correctionCount(state.correctionCount())
                .reviewIssues(state.reviewIssues())
                .build();
    }
}
