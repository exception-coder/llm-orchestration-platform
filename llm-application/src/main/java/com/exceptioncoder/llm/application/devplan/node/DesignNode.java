package com.exceptioncoder.llm.application.devplan.node;

import com.exceptioncoder.llm.domain.devplan.model.AgentOutput;
import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanDocument;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.service.DevPlanAgentRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 方案生成节点
 * 编排逻辑：调用 SolutionArchitect Agent → 写入 document 到 State
 */
@Slf4j
@Component
public class DesignNode {

    private final DevPlanAgentRouter agentRouter;

    public DesignNode(DevPlanAgentRouter agentRouter) {
        this.agentRouter = agentRouter;
    }

    public DevPlanState execute(DevPlanState state) {
        log.info("执行 DesignNode，taskId={}，correctionCount={}",
                state.taskId(), state.correctionCount());
        long start = System.currentTimeMillis();

        AgentOutput output = agentRouter.route(AgentRole.SOLUTION_ARCHITECT, state);

        long elapsed = System.currentTimeMillis() - start;
        var timings = new java.util.HashMap<>(state.nodeTimings());
        timings.put("design", elapsed);
        var tokenUsage = new java.util.HashMap<>(state.agentTokenUsage());
        tokenUsage.put(AgentRole.SOLUTION_ARCHITECT.name(), output.tokenUsage());

        DevPlanDocument document = (DevPlanDocument) output.structuredData().get("document");
        if (document == null) {
            document = new DevPlanDocument(output.rawOutput(), java.util.Map.of());
        }

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
