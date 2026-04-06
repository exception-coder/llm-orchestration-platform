package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.application.devplan.DevPlanFlowDefinition;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanTask;
import com.exceptioncoder.llm.domain.devplan.service.DevPlanTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 开发方案生成用例
 * 顶层入口：提交任务 → 启动流程 → 组装响应
 */
@Slf4j
@Service
public class DevPlanUseCase {

    private final DevPlanTaskManager taskManager;
    private final DevPlanFlowDefinition flowDefinition;

    public DevPlanUseCase(DevPlanTaskManager taskManager,
                          DevPlanFlowDefinition flowDefinition) {
        this.taskManager = taskManager;
        this.flowDefinition = flowDefinition;
    }

    /**
     * 生成开发方案
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
     * 查询任务状态
     */
    public Optional<DevPlanTask> getTaskStatus(String taskId) {
        return taskManager.getTask(taskId);
    }
}
