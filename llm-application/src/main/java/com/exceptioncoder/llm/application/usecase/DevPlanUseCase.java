package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.application.devplan.DevPlanFlowDefinition;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanTask;
import com.exceptioncoder.llm.domain.devplan.service.DevPlanTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 开发方案生成用例（DevPlanUseCase）—— devplan 模块的顶层应用服务入口。
 *
 * <p>本类属于 <b>应用层（Application Layer）</b>，是 devplan 模块对外暴露的核心用例。
 * 它编排任务管理与流程执行两大关注点：提交任务 → 构建初始状态 → 带超时执行流程 → 标记完成/失败。</p>
 *
 * <h3>设计思路</h3>
 * <ul>
 *   <li>将任务生命周期管理（并发检查、超时控制、状态追踪）委托给 {@link DevPlanTaskManager}</li>
 *   <li>将流程编排逻辑委托给 {@link DevPlanFlowDefinition}，本类只做"用例级"编排</li>
 *   <li>异常时自动将任务标记为失败，确保任务状态的一致性</li>
 * </ul>
 *
 * <h3>协作关系</h3>
 * <ul>
 *   <li>{@link DevPlanTaskManager} — 任务管理器，负责任务的创建、超时执行、状态查询和生命周期管理</li>
 *   <li>{@link DevPlanFlowDefinition} — 流程定义，编排 ScanNode → AnalyzeNode → DesignNode ⇄ ReviewNode</li>
 *   <li>{@link com.exceptioncoder.llm.api.controller.DevPlanController} — API 层控制器，调用本用例</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Service
public class DevPlanUseCase {

    /** 任务管理器 —— 负责任务的创建、超时执行、状态查询和生命周期管理 */
    private final DevPlanTaskManager taskManager;
    /** 流程定义 —— 编排四个节点的执行序列和条件路由 */
    private final DevPlanFlowDefinition flowDefinition;

    /**
     * 构造 DevPlanUseCase 实例。
     *
     * @param taskManager    任务管理器
     * @param flowDefinition 流程定义
     */
    public DevPlanUseCase(DevPlanTaskManager taskManager,
                          DevPlanFlowDefinition flowDefinition) {
        this.taskManager = taskManager;
        this.flowDefinition = flowDefinition;
    }

    /**
     * 生成开发方案 —— 从需求到方案文档的完整用例。
     *
     * <p>执行步骤：</p>
     * <ol>
     *   <li>提交任务到控制平面，进行并发检查（同一项目不允许同时运行多个任务）</li>
     *   <li>构建包含 taskId、projectPath、requirement 的初始状态</li>
     *   <li>带超时保护执行完整流程（Scan → Analyze → Design ⇄ Review）</li>
     *   <li>成功时标记任务完成，异常时标记任务失败并抛出运行时异常</li>
     * </ol>
     *
     * @param projectPath    目标项目路径，用于代码感知扫描
     * @param requirement    用户需求描述文本
     * @param timeoutSeconds 流程执行超时时间（秒）
     * @return 流程执行完成后的最终状态，包含方案文档、验证结果等全部产出
     * @throws RuntimeException 流程执行失败时抛出，包含失败原因
     */
    public DevPlanState generateDevPlan(String projectPath, String requirement, int timeoutSeconds) {
        log.info("收到方案生成请求，projectPath={}，requirement={}",
                projectPath, requirement.substring(0, Math.min(100, requirement.length())));

        // 1. 提交任务到控制平面（含并发检查）
        DevPlanTask task = taskManager.submitTask(projectPath, requirement, timeoutSeconds);

        try {
            // 2. 构建初始状态
            DevPlanState initialState = DevPlanState.builder()
                    .taskId(task.taskId())
                    .projectPath(projectPath)
                    .requirement(requirement)
                    .status("RUNNING")
                    .build();

            // 3. 带超时执行流程
            DevPlanState result = taskManager.executeWithTimeout(
                    () -> flowDefinition.execute(initialState),
                    timeoutSeconds
            );

            // 4. 标记完成
            taskManager.completeTask(task.taskId());
            return result;

        } catch (Exception e) {
            taskManager.failTask(task.taskId(), e.getMessage());
            throw new RuntimeException("方案生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询指定任务的当前状态。
     *
     * @param taskId 任务唯一标识
     * @return 包含任务信息的 Optional，任务不存在时返回 {@link Optional#empty()}
     */
    public Optional<DevPlanTask> getTaskStatus(String taskId) {
        return taskManager.getTask(taskId);
    }
}
