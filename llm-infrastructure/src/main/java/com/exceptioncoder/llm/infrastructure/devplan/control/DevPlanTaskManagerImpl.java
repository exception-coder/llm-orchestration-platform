package com.exceptioncoder.llm.infrastructure.devplan.control;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanTask;
import com.exceptioncoder.llm.domain.devplan.repository.DevPlanTaskRepository;
import com.exceptioncoder.llm.domain.devplan.service.DevPlanTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务管理实现 — 控制平面核心
 */
@Slf4j
@Service
public class DevPlanTaskManagerImpl implements DevPlanTaskManager {

    private final DevPlanTaskRepository taskRepository;
    private final ConcurrencyController concurrencyController;
    private final TimeoutController timeoutController;
    private final AtomicLong taskCounter = new AtomicLong(0);

    public DevPlanTaskManagerImpl(DevPlanTaskRepository taskRepository,
                                  ConcurrencyController concurrencyController,
                                  TimeoutController timeoutController) {
        this.taskRepository = taskRepository;
        this.concurrencyController = concurrencyController;
        this.timeoutController = timeoutController;
    }

    @Override
    public DevPlanTask submitTask(String projectPath, String requirement, int timeoutSeconds) {
        // 并发检查
        concurrencyController.acquire();

        String taskId = generateTaskId();
        DevPlanTask task = DevPlanTask.builder()
                .taskId(taskId)
                .projectPath(projectPath)
                .requirement(requirement)
                .status(DevPlanTask.Status.RUNNING.name())
                .timeoutSeconds(timeoutSeconds)
                .startedAt(LocalDateTime.now())
                .build();

        task = taskRepository.save(task);
        log.info("任务已创建，taskId={}", taskId);
        return task;
    }

    @Override
    public Optional<DevPlanTask> getTask(String taskId) {
        return taskRepository.findByTaskId(taskId);
    }

    @Override
    public void completeTask(String taskId) {
        taskRepository.updateStatus(taskId, DevPlanTask.Status.COMPLETED.name(), null);
        concurrencyController.release();
        log.info("任务完成，taskId={}", taskId);
    }

    @Override
    public void failTask(String taskId, String errorMessage) {
        taskRepository.updateStatus(taskId, DevPlanTask.Status.FAILED.name(), null);
        concurrencyController.release();
        log.error("任务失败，taskId={}，error={}", taskId, errorMessage);
    }

    @Override
    public <T> T executeWithTimeout(Callable<T> callable, int timeoutSeconds) {
        return timeoutController.executeWithTimeout(callable, timeoutSeconds);
    }

    private String generateTaskId() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "dp-" + date + "-" + String.format("%03d", taskCounter.incrementAndGet());
    }
}
