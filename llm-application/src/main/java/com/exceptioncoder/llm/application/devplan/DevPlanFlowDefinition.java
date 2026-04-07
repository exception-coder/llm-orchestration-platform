package com.exceptioncoder.llm.application.devplan;

import com.exceptioncoder.llm.application.devplan.node.AnalyzeNode;
import com.exceptioncoder.llm.application.devplan.node.DesignNode;
import com.exceptioncoder.llm.application.devplan.node.ReviewNode;
import com.exceptioncoder.llm.application.devplan.node.ScanNode;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.service.DevPlanMemoryManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 开发方案生成流程定义（StateGraph 编排器）。
 *
 * <p>本类属于 <b>应用层（Application Layer）</b>，是 devplan 模块的流程编排核心。
 * 它将四个处理节点按照有向图的方式串联，并在 DesignNode 与 ReviewNode 之间
 * 实现条件路由的修正循环，从而保证生成的方案质量达标后才输出。</p>
 *
 * <h3>流程拓扑</h3>
 * <pre>
 * START → ScanNode → AnalyzeNode → DesignNode → ReviewNode → END
 *                                       ↑                |
 *                                       └── review.failed ─┘
 * </pre>
 *
 * <h3>设计思路</h3>
 * <ul>
 *   <li>借鉴 LangGraph StateGraph 思想，用不可变 State 在节点间传递上下文</li>
 *   <li>条件路由委托给 {@link ReviewRoutingStrategy}，实现策略与编排解耦</li>
 *   <li>流程执行完毕后通过 {@link DevPlanMemoryManager} 持久化记忆，供后续增量分析使用</li>
 * </ul>
 *
 * <h3>协作关系</h3>
 * <ul>
 *   <li>{@link ScanNode} — 代码感知节点，扫描项目结构与架构拓扑</li>
 *   <li>{@link AnalyzeNode} — 需求分析节点，提取需求意图与影响面</li>
 *   <li>{@link DesignNode} — 方案生成节点，调用 LLM 生成开发方案文档</li>
 *   <li>{@link ReviewNode} — 方案审查节点，对方案进行质量评分</li>
 *   <li>{@link ReviewRoutingStrategy} — 审查路由策略，决定通过/重试/带问题通过</li>
 *   <li>{@link DevPlanMemoryManager} — 记忆管理器，持久化最终状态</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Component
public class DevPlanFlowDefinition {

    /** 代码感知节点 —— 扫描项目结构和架构拓扑 */
    private final ScanNode scanNode;
    /** 需求分析节点 —— 提取需求意图与影响面 */
    private final AnalyzeNode analyzeNode;
    /** 方案生成节点 —— 调用 LLM 输出开发方案文档 */
    private final DesignNode designNode;
    /** 方案审查节点 —— 对方案进行质量评审 */
    private final ReviewNode reviewNode;
    /** 审查条件路由策略 —— 决定审查后的流程走向 */
    private final ReviewRoutingStrategy reviewRoutingStrategy;
    /** 记忆管理器 —— 持久化流程执行结果供后续增量使用 */
    private final DevPlanMemoryManager memoryManager;

    public DevPlanFlowDefinition(ScanNode scanNode,
                                  AnalyzeNode analyzeNode,
                                  DesignNode designNode,
                                  ReviewNode reviewNode,
                                  ReviewRoutingStrategy reviewRoutingStrategy,
                                  DevPlanMemoryManager memoryManager) {
        this.scanNode = scanNode;
        this.analyzeNode = analyzeNode;
        this.designNode = designNode;
        this.reviewNode = reviewNode;
        this.reviewRoutingStrategy = reviewRoutingStrategy;
        this.memoryManager = memoryManager;
    }

    /**
     * 执行完整的开发方案生成流程。
     *
     * <p>按照 ScanNode → AnalyzeNode → DesignNode ⇄ ReviewNode 的顺序依次执行，
     * 其中 DesignNode 和 ReviewNode 之间可能因审查不通过而进入修正循环。
     * 流程结束后会将最终状态持久化到记忆存储。</p>
     *
     * @param initialState 初始流程状态，包含 taskId、projectPath、requirement 等基础信息
     * @return 流程执行完成后的最终状态，包含方案文档、验证结果、耗时统计等全部产出
     */
    public DevPlanState execute(DevPlanState initialState) {
        log.info("开始执行开发方案流程，taskId={}", initialState.taskId());

        // Node 1: 代码感知
        DevPlanState state = scanNode.execute(initialState);

        // Node 2: 需求分析
        state = analyzeNode.execute(state);

        // Node 3 + 4: 方案生成 + 审查（含修正循环）
        state = executeDesignReviewLoop(state);

        // 持久化记忆
        memoryManager.persist(state.taskId(), state);

        log.info("流程执行完成，taskId={}，score={}",
                state.taskId(),
                state.validation() != null ? state.validation().score() : "N/A");
        return state;
    }

    /**
     * 执行方案生成与审查的修正循环。
     *
     * <p>循环流程：DesignNode 生成方案 → ReviewNode 审查 → ReviewRoutingStrategy 决策。
     * 若审查不通过且修正次数未超限，则回退到 DesignNode 重新生成。
     * 循环退出条件：审查通过、修正次数超限（带问题通过）、或未知决策。</p>
     *
     * @param state 进入循环前的流程状态，已包含代码感知和需求分析结果
     * @return 循环结束后的最终状态，包含方案文档和审查验证结果
     */
    private DevPlanState executeDesignReviewLoop(DevPlanState state) {
        while (true) {
            // Node 3: 方案生成
            state = designNode.execute(state);

            // Node 4: 方案审查
            state = reviewNode.execute(state);

            // 条件路由：根据审查结果决定继续循环还是退出
            String decision = reviewRoutingStrategy.route(state);
            switch (decision) {
                case ReviewRoutingStrategy.APPROVED:
                    log.info("方案审查通过，taskId={}", state.taskId());
                    return state;
                case ReviewRoutingStrategy.APPROVED_WITH_ISSUES:
                    log.warn("修正次数超限，带问题通过，taskId={}", state.taskId());
                    return state;
                case ReviewRoutingStrategy.RETRY_DESIGN:
                    log.info("方案审查不通过，回退到 DesignNode，correctionCount={}",
                            state.correctionCount());
                    break;
                default:
                    return state;
            }
        }
    }
}
