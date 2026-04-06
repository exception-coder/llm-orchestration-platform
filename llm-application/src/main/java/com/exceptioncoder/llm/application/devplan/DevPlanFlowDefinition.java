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
 * StateGraph 流程定义
 * 定义 4 个 Node 的执行序列和条件路由
 *
 * START → ScanNode → AnalyzeNode → DesignNode → ReviewNode → END
 *                                       ↑                |
 *                                       └── review.failed ─┘
 */
@Slf4j
@Component
public class DevPlanFlowDefinition {

    private final ScanNode scanNode;
    private final AnalyzeNode analyzeNode;
    private final DesignNode designNode;
    private final ReviewNode reviewNode;
    private final ReviewRoutingStrategy reviewRoutingStrategy;
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
     * 执行完整流程
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

    private DevPlanState executeDesignReviewLoop(DevPlanState state) {
        while (true) {
            // Node 3: 方案生成
            state = designNode.execute(state);

            // Node 4: 方案审查
            state = reviewNode.execute(state);

            // 条件路由
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
