package com.exceptioncoder.llm.application.devplan.node;

import com.exceptioncoder.llm.domain.devplan.model.AgentOutput;
import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.model.ValidationResult;
import com.exceptioncoder.llm.domain.devplan.service.DevPlanAgentRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 方案审查节点（ReviewNode）—— 开发方案生成流程的第四个执行节点。
 *
 * <p>本类属于 <b>应用层（Application Layer）</b>，是 devplan 模块 StateGraph 的 Node 4。
 * 职责是调用 {@link AgentRole#PLAN_REVIEWER} Agent 对 DesignNode 生成的开发方案进行
 * 质量评审，产出验证结果（{@link ValidationResult}），包括评分、是否通过和问题列表。
 * 审查结果将驱动 {@link ReviewRoutingStrategy} 进行路由决策。</p>
 *
 * <h3>设计思路</h3>
 * <ul>
 *   <li>审查不通过时，本节点会递增 {@code correctionCount} 并将问题列表写入 State</li>
 *   <li>问题列表 {@code reviewIssues} 会在修正循环中传递给 DesignNode，指导方案修正</li>
 *   <li>审查通过时不递增修正计数，保持原值</li>
 * </ul>
 *
 * <h3>协作关系</h3>
 * <ul>
 *   <li>{@link DevPlanAgentRouter} — Agent 路由器，将请求分发到 PLAN_REVIEWER Agent</li>
 *   <li>{@link DevPlanFlowDefinition} — 流程编排器，在修正循环中调用本节点</li>
 *   <li>{@link DesignNode} — 上游方案生成节点，产出本节点需要审查的方案文档</li>
 *   <li>{@link ReviewRoutingStrategy} — 根据本节点产出的审查结果进行路由决策</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Component
public class ReviewNode {

    /** Agent 路由器 —— 将请求分发到对应角色的 Agent 实现 */
    private final DevPlanAgentRouter agentRouter;

    /**
     * 构造 ReviewNode 实例。
     *
     * @param agentRouter Agent 路由器，用于将请求分发到 PLAN_REVIEWER Agent
     */
    public ReviewNode(DevPlanAgentRouter agentRouter) {
        this.agentRouter = agentRouter;
    }

    /**
     * 执行方案审查。
     *
     * <p>处理步骤：</p>
     * <ol>
     *   <li>调用 PLAN_REVIEWER Agent 对当前方案文档进行审查</li>
     *   <li>从 Agent 输出中提取 {@link ValidationResult}，获取评分和问题列表</li>
     *   <li>若审查不通过，递增修正计数 {@code correctionCount}</li>
     *   <li>记录节点执行耗时和 Token 使用量</li>
     *   <li>构建新的 {@link DevPlanState} 并返回，状态标记为 {@code REVIEWING_COMPLETE}</li>
     * </ol>
     *
     * @param state 上游传入的流程状态，需包含 DesignNode 产出的方案文档
     * @return 包含审查验证结果和更新后修正计数的新状态
     */
    public DevPlanState execute(DevPlanState state) {
        log.info("执行 ReviewNode，taskId={}", state.taskId());
        long start = System.currentTimeMillis();

        // 1. 调用 PLAN_REVIEWER Agent 对方案文档进行质量评审
        AgentOutput output = agentRouter.route(AgentRole.PLAN_REVIEWER, state);

        // 2. 记录节点执行耗时和 Token 使用量
        long elapsed = System.currentTimeMillis() - start;
        var timings = new java.util.HashMap<>(state.nodeTimings());
        timings.put("review", elapsed);
        var tokenUsage = new java.util.HashMap<>(state.agentTokenUsage());
        tokenUsage.put(AgentRole.PLAN_REVIEWER.name(), output.tokenUsage());

        // 3. 提取审查结果，获取问题列表（无审查结果时返回空列表）
        ValidationResult validation = (ValidationResult) output.structuredData().get("validation");
        List<String> issues = validation != null ? validation.issues() : List.of();

        // 4. 审查不通过时递增修正计数，通过时保持原值
        int newCorrectionCount = (validation != null && !validation.passed())
                ? state.correctionCount() + 1
                : state.correctionCount();

        // 5. 构建新的不可变 State，携带审查结果供路由策略决策
        return DevPlanState.builder()
                .taskId(state.taskId())
                .projectPath(state.projectPath())
                .requirement(state.requirement())
                .structure(state.structure())
                .topology(state.topology())
                .intent(state.intent())
                .impact(state.impact())
                .document(state.document())
                .validation(validation)
                .reviewIssues(issues)
                .correctionCount(newCorrectionCount)
                .status("REVIEWING_COMPLETE")
                .nodeTimings(timings)
                .agentTokenUsage(tokenUsage)
                .build();
    }
}
