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
 * 方案审查节点
 * 编排逻辑：调用 PlanReviewer Agent → 写入 validation 到 State
 */
@Slf4j
@Component
public class ReviewNode {

    private final DevPlanAgentRouter agentRouter;

    public ReviewNode(DevPlanAgentRouter agentRouter) {
        this.agentRouter = agentRouter;
    }

    public DevPlanState execute(DevPlanState state) {
        log.info("执行 ReviewNode，taskId={}", state.taskId());
        long start = System.currentTimeMillis();

        AgentOutput output = agentRouter.route(AgentRole.PLAN_REVIEWER, state);

        long elapsed = System.currentTimeMillis() - start;
        var timings = new java.util.HashMap<>(state.nodeTimings());
        timings.put("review", elapsed);
        var tokenUsage = new java.util.HashMap<>(state.agentTokenUsage());
        tokenUsage.put(AgentRole.PLAN_REVIEWER.name(), output.tokenUsage());

        ValidationResult validation = (ValidationResult) output.structuredData().get("validation");
        List<String> issues = validation != null ? validation.issues() : List.of();
        int newCorrectionCount = (validation != null && !validation.passed())
                ? state.correctionCount() + 1
                : state.correctionCount();

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
