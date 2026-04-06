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
 * 开发计划任务管理器实现，控制平面的核心协调组件。
 *
 * <p>属于 Infrastructure层 devplan/control 模块，实现了 Domain 层定义的
 * {@link DevPlanTaskManager} 接口，负责任务的全生命周期管理：
 * 创建 → 执行（带并发 + 超时控制） → 完成/失败。
 *
 * <p><b>设计思路：</b>本类不直接实现并发控制和超时控制逻辑，
 * 而是将这两个关注点分别委托给 {@link ConcurrencyController} 和 {@link TimeoutController}，
 * 遵循单一职责原则，自身只聚焦于任务状态流转和持久化编排。
 *
 * <p><b>协作关系：</b>
 * <ul>
 *   <li>实现 {@link DevPlanTaskManager}（Domain 层接口）</li>
 *   <li>依赖 {@link DevPlanTaskRepository} 进行任务持久化</li>
 *   <li>依赖 {@link ConcurrencyController} 进行并发槽位管理</li>
 *   <li>依赖 {@link TimeoutController} 进行超时保护</li>
 *   <li>被 Application 层的用例服务调用</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Service
public class DevPlanTaskManagerImpl implements DevPlanTaskManager {

    /** 任务持久化仓储 */
    private final DevPlanTaskRepository taskRepository;
    /** 并发控制器，管理任务执行的并发槽位 */
    private final ConcurrencyController concurrencyController;
    /** 超时控制器，为任务执行施加时间边界 */
    private final TimeoutController timeoutController;
    /** 任务序号计数器，用于生成自增的任务 ID 后缀（进程内唯一） */
    private final AtomicLong taskCounter = new AtomicLong(0);

    /**
     * 构造任务管理器，注入所需的协作组件。
     *
     * @param taskRepository        任务持久化仓储
     * @param concurrencyController 并发控制器
     * @param timeoutController     超时控制器
     */
    public DevPlanTaskManagerImpl(DevPlanTaskRepository taskRepository,
                                  ConcurrencyController concurrencyController,
                                  TimeoutController timeoutController) {
        this.taskRepository = taskRepository;
        this.concurrencyController = concurrencyController;
        this.timeoutController = timeoutController;
    }

    /**
     * 提交一个新的开发计划任务。
     *
     * <p>执行流程：先获取并发槽位（失败则快速拒绝），再生成唯一任务 ID，
     * 构建任务对象并持久化到仓储。
     *
     * @param projectPath    目标项目路径
     * @param requirement    用户需求描述
     * @param timeoutSeconds 任务超时时间（秒）
     * @return 创建后的任务对象（包含生成的 taskId）
     * @throws ConcurrencyController.ConcurrencyExceededException 并发槽位已满时抛出
     */
    @Override
    public DevPlanTask submitTask(String projectPath, String requirement, int timeoutSeconds) {
        // 第一步：获取并发槽位，超限则快速失败
        concurrencyController.acquire();

        // 第二步：生成唯一任务 ID 并构建任务对象
        String taskId = generateTaskId();
        DevPlanTask task = DevPlanTask.builder()
                .taskId(taskId)
                .projectPath(projectPath)
                .requirement(requirement)
                .status(DevPlanTask.Status.RUNNING.name())
                .timeoutSeconds(timeoutSeconds)
                .startedAt(LocalDateTime.now())
                .build();

        // 第三步：持久化任务
        task = taskRepository.save(task);
        log.info("任务已创建，taskId={}", taskId);
        return task;
    }

    /**
     * 根据任务 ID 查询任务详情。
     *
     * @param taskId 任务唯一标识
     * @return 包含任务信息的 Optional，任务不存在时返回 empty
     */
    @Override
    public Optional<DevPlanTask> getTask(String taskId) {
        return taskRepository.findByTaskId(taskId);
    }

    /**
     * 将任务标记为完成状态，并释放并发槽位。
     *
     * @param taskId 任务唯一标识
     */
    @Override
    public void completeTask(String taskId) {
        taskRepository.updateStatus(taskId, DevPlanTask.Status.COMPLETED.name(), null);
        // 释放并发槽位，允许新任务提交
        concurrencyController.release();
        log.info("任务完成，taskId={}", taskId);
    }

    /**
     * 将任务标记为失败状态，并释放并发槽位。
     *
     * @param taskId       任务唯一标识
     * @param errorMessage 失败原因描述
     */
    @Override
    public void failTask(String taskId, String errorMessage) {
        taskRepository.updateStatus(taskId, DevPlanTask.Status.FAILED.name(), null);
        // 即使失败也必须释放槽位，否则会导致槽位泄漏
        concurrencyController.release();
        log.error("任务失败，taskId={}，error={}", taskId, errorMessage);
    }

    /**
     * 在超时保护下执行指定的任务逻辑。
     *
     * @param callable       需要执行的任务逻辑
     * @param timeoutSeconds 超时时间（秒）
     * @param <T>            任务返回值类型
     * @return 任务执行结果
     * @throws TimeoutController.TaskTimeoutException 任务超时时抛出
     */
    @Override
    public <T> T executeWithTimeout(Callable<T> callable, int timeoutSeconds) {
        return timeoutController.executeWithTimeout(callable, timeoutSeconds);
    }

    /**
     * 生成任务唯一标识。
     *
     * <p>格式为 {@code dp-yyyyMMdd-xxx}，其中 xxx 为三位零填充的自增序号。
     * 注意：taskCounter 为进程内计数器，重启后会归零，适用于一期内存存储方案；
     * 后续切换持久化存储时需改为数据库序列或分布式 ID 生成器。
     *
     * @return 生成的任务 ID
     */
    private String generateTaskId() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "dp-" + date + "-" + String.format("%03d", taskCounter.incrementAndGet());
    }
}
