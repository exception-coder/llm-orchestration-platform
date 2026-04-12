package com.exceptioncoder.llm.application.devplan.node;

import com.exceptioncoder.llm.domain.devplan.model.AgentOutput;
import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.model.ImpactAnalysis;
import com.exceptioncoder.llm.domain.devplan.model.RequirementIntent;
import com.exceptioncoder.llm.domain.devplan.service.DevPlanAgentRouter;
import com.exceptioncoder.llm.domain.devplan.service.ProfileReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 需求分析节点（AnalyzeNode）—— 开发方案生成流程的第二个执行节点。
 *
 * <p>v2 改造：加载 {@code business-context.md} 作为需求分析的核心上下文，
 * <b>不加载</b> {@code coding-conventions.md}，避免编码约定干扰需求分析产生幻觉。</p>
 *
 * <p>当 {@code business-context.md} 不存在时，自动降级为 v1 模式（纯搜索推理），
 * 并在输出中标注 {@code degraded=true}。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Component
public class AnalyzeNode {

    private final DevPlanAgentRouter agentRouter;
    private final ProfileReader profileReader;

    public AnalyzeNode(DevPlanAgentRouter agentRouter, ProfileReader profileReader) {
        this.agentRouter = agentRouter;
        this.profileReader = profileReader;
    }

    /**
     * 执行需求分析。
     *
     * <p>处理步骤：</p>
     * <ol>
     *   <li>加载 business-context.md（不存在则降级）</li>
     *   <li>将 businessContext 注入 State，调用 REQUIREMENT_ANALYZER Agent</li>
     *   <li>从 Agent 输出中提取 {@link ImpactAnalysis}</li>
     *   <li>构建新的 {@link DevPlanState} 并返回</li>
     * </ol>
     */
    public DevPlanState execute(DevPlanState state) {
        log.info("执行 AnalyzeNode，taskId={}", state.taskId());
        long start = System.currentTimeMillis();

        // 1. 加载 business-context.md（按需加载，不加载 coding-conventions.md）
        String businessContext = loadBusinessContext(state.projectPath());

        // 2. 将 businessContext 注入 State，供 Agent Prompt 消费
        DevPlanState enrichedState = DevPlanState.builder()
                .taskId(state.taskId())
                .projectPath(state.projectPath())
                .requirement(state.requirement())
                .projectProfile(state.projectProfile())
                .businessContext(businessContext)
                .structure(state.structure())
                .topology(state.topology())
                .status(state.status())
                .nodeTimings(state.nodeTimings())
                .agentTokenUsage(state.agentTokenUsage())
                .correctionCount(state.correctionCount())
                .reviewIssues(state.reviewIssues())
                .build();

        // 3. 调用 REQUIREMENT_ANALYZER Agent
        AgentOutput output = agentRouter.route(AgentRole.REQUIREMENT_ANALYZER, enrichedState);

        // 4. 记录耗时和 Token
        long elapsed = System.currentTimeMillis() - start;
        var timings = new java.util.HashMap<>(state.nodeTimings());
        timings.put("analyze", elapsed);
        var tokenUsage = new java.util.HashMap<>(state.agentTokenUsage());
        tokenUsage.put(AgentRole.REQUIREMENT_ANALYZER.name(), output.tokenUsage());

        // 5. 提取结构化输出
        RequirementIntent intent = (RequirementIntent) output.structuredData().get("intent");
        ImpactAnalysis impact = (ImpactAnalysis) output.structuredData().get("impact");

        log.info("AnalyzeNode 完成，taskId={}，耗时={}ms，degraded={}",
                state.taskId(), elapsed, businessContext == null);

        // 6. 构建新 State
        return DevPlanState.builder()
                .taskId(state.taskId())
                .projectPath(state.projectPath())
                .requirement(state.requirement())
                .projectProfile(state.projectProfile())
                .businessContext(businessContext)
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

    /**
     * 加载 business-context.md。不存在时返回 null（触发降级到 v1 纯搜索模式）。
     */
    private String loadBusinessContext(String projectPath) {
        Path contextPath = Path.of(projectPath, "docs", "business-context.md");
        try {
            String content = profileReader.readFull(contextPath);
            log.info("business-context.md loaded, {} chars", content.length());
            return content;
        } catch (IOException e) {
            log.warn("business-context.md not found at {}, degrading to v1 mode", contextPath);
            return null;
        }
    }
}
