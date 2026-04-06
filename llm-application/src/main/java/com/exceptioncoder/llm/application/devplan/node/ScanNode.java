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
 * 代码感知节点
 * 编排逻辑：调用 CodeAwareness Agent → 写入 structure + topology 到 State
 */
@Slf4j
@Component
public class ScanNode {

    private final DevPlanAgentRouter agentRouter;

    public ScanNode(DevPlanAgentRouter agentRouter) {
        this.agentRouter = agentRouter;
    }

    public DevPlanState execute(DevPlanState state) {
        log.info("执行 ScanNode，projectPath={}", state.projectPath());
        long start = System.currentTimeMillis();

        AgentOutput output = agentRouter.route(AgentRole.CODE_AWARENESS, state);

        long elapsed = System.currentTimeMillis() - start;
        var timings = new java.util.HashMap<>(state.nodeTimings());
        timings.put("scan", elapsed);
        var tokenUsage = new java.util.HashMap<>(state.agentTokenUsage());
        tokenUsage.put(AgentRole.CODE_AWARENESS.name(), output.tokenUsage());

        ProjectStructure structure = (ProjectStructure) output.structuredData().get("structure");
        ArchTopology topology = (ArchTopology) output.structuredData().get("topology");

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
