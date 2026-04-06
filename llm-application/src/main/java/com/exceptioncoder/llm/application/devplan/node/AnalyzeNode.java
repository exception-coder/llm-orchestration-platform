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
 * 需求分析节点
 * 编排逻辑：调用 RequirementAnalyzer Agent → 写入 intent + impact 到 State
 */
@Slf4j
@Component
public class AnalyzeNode {

    private final DevPlanAgentRouter agentRouter;

    public AnalyzeNode(DevPlanAgentRouter agentRouter) {
        this.agentRouter = agentRouter;
    }

    public DevPlanState execute(DevPlanState state) {
        log.info("执行 AnalyzeNode，taskId={}", state.taskId());
        long start = System.currentTimeMillis();

        AgentOutput output = agentRouter.route(AgentRole.REQUIREMENT_ANALYZER, state);

        long elapsed = System.currentTimeMillis() - start;
        var timings = new java.util.HashMap<>(state.nodeTimings());
        timings.put("analyze", elapsed);
        var tokenUsage = new java.util.HashMap<>(state.agentTokenUsage());
        tokenUsage.put(AgentRole.REQUIREMENT_ANALYZER.name(), output.tokenUsage());

        RequirementIntent intent = (RequirementIntent) output.structuredData().get("intent");
        ImpactAnalysis impact = (ImpactAnalysis) output.structuredData().get("impact");

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
