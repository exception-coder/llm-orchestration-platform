package com.exceptioncoder.llm.infrastructure.agent.task;

import com.exceptioncoder.llm.domain.executor.AgentExecutor;
import com.exceptioncoder.llm.domain.executor.AgentIterationListener;
import com.exceptioncoder.llm.domain.model.*;
import com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agent 任务管理器实现。
 *
 * <p>编排并发控制（Semaphore） + 线程池异步执行 + 超时控制（Future.get）。
 * 复用 DevPlan 模块已验证的 TaskManager 架构模式。</p>
 */
@Slf4j
@Component
public class AgentTaskManagerImpl implements AgentTaskManager {

    private final AgentTaskStore taskStore;
    private final AgentExecutor agentExecutor;
    private final AgentDefinitionRepository agentRepository;
    private final AgentIterationListener iterationListener;
    private final ExecutorService executorPool;
    private final Semaphore concurrencySemaphore;
    private final AgentAsyncConfig config;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final AtomicLong taskCounter = new AtomicLong(0);

    public AgentTaskManagerImpl(
            AgentTaskStore taskStore,
            AgentExecutor agentExecutor,
            AgentDefinitionRepository agentRepository,
            AgentIterationListener iterationListener,
            @Qualifier("agentExecutorPool") ExecutorService executorPool,
            @Qualifier("agentConcurrencySemaphore") Semaphore concurrencySemaphore,
            AgentAsyncConfig config
    ) {
        this.taskStore = taskStore;
        this.agentExecutor = agentExecutor;
        this.agentRepository = agentRepository;
        this.iterationListener = iterationListener;
        this.executorPool = executorPool;
        this.concurrencySemaphore = concurrencySemaphore;
        this.config = config;
    }

    @Override
    public AgentTask submit(String agentId, String input, Map<String, Object> context, int timeoutSeconds) {
        // 1. 并发检查
        if (!concurrencySemaphore.tryAcquire()) {
            log.warn("Agent 并发超限，拒绝执行: agentId={}", agentId);
            return null;
        }

        // 2. 查询 Agent 定义获取 maxIterations
        AgentDefinition agent = agentRepository.findById(agentId)
                .orElseThrow(() -> {
                    concurrencySemaphore.release();
                    return new IllegalArgumentException("Agent 不存在: " + agentId);
                });

        // 3. 生成 executionId 并创建任务
        String executionId = generateExecutionId();
        AgentTask task = AgentTask.builder()
                .executionId(executionId)
                .agentId(agentId)
                .status(AgentTask.Status.SUBMITTED)
                .maxIterations(agent.maxIterations())
                .build();

        taskStore.save(task);
        log.info("Agent 任务已提交: executionId={}, agentId={}", executionId, agentId);

        // 4. 提交到线程池异步执行
        Future<?> future = executorPool.submit(() -> executeTask(executionId, agentId, input, context));

        // 5. 启动超时监控线程
        startTimeoutWatcher(executionId, future, timeoutSeconds);

        return task;
    }

    @Override
    public Optional<AgentTask> getTask(String executionId) {
        return taskStore.get(executionId);
    }

    @Override
    public void completeTask(String executionId, AgentExecutionResult result) {
        taskStore.update(executionId, existing -> existing.toBuilder()
                .status(AgentTask.Status.COMPLETED)
                .finalOutput(result.finalOutput())
                .thoughtHistory(result.thoughtHistory())
                .toolCalls(result.toolCalls())
                .currentIteration(result.iterations())
                .completedAt(LocalDateTime.now())
                .build());
        log.info("Agent 执行完成: executionId={}, iterations={}, elapsed={}ms",
                executionId, result.iterations(), result.elapsedMs());
    }

    @Override
    public void failTask(String executionId, String errorMessage, AgentTask.Status failStatus) {
        taskStore.update(executionId, existing -> existing.toBuilder()
                .status(failStatus)
                .errorMessage(errorMessage)
                .completedAt(LocalDateTime.now())
                .build());
        log.error("Agent 执行失败: executionId={}, status={}, error={}", executionId, failStatus, errorMessage);
    }

    private void executeTask(String executionId, String agentId, String input, Map<String, Object> context) {
        try {
            // 更新状态为 RUNNING
            taskStore.update(executionId, existing -> existing.toBuilder()
                    .status(AgentTask.Status.RUNNING)
                    .startedAt(LocalDateTime.now())
                    .build());

            // 构建执行请求
            AgentExecutor.AgentExecutionRequest request = AgentExecutor.AgentExecutionRequest.builder()
                    .executionId(executionId)
                    .agentId(agentId)
                    .userInput(input)
                    .context(context != null ? context : Map.of())
                    .build();

            // 执行（带迭代回调）
            AgentExecutionResult result = agentExecutor.execute(request, iterationListener);

            // 标记完成
            if (result.status() == AgentExecutionResult.Status.SUCCESS) {
                completeTask(executionId, result);
                iterationListener.onComplete(executionId, result);
            } else {
                failTask(executionId, result.errorMessage(), AgentTask.Status.FAILED);
                iterationListener.onError(executionId, result.errorMessage());
            }
        } catch (Exception e) {
            log.error("Agent 任务执行异常: executionId={}", executionId, e);
            failTask(executionId, e.getMessage(), AgentTask.Status.FAILED);
            iterationListener.onError(executionId, e.getMessage());
        } finally {
            concurrencySemaphore.release();
        }
    }

    private void startTimeoutWatcher(String executionId, Future<?> future, int timeoutSeconds) {
        Thread timeoutThread = new Thread(() -> {
            try {
                future.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.warn("Agent 执行超时，强制取消: executionId={}, timeout={}s", executionId, timeoutSeconds);
                future.cancel(true);
                failTask(executionId, "执行超时（" + timeoutSeconds + "秒）", AgentTask.Status.TIMED_OUT);
                iterationListener.onError(executionId, "执行超时");
                concurrencySemaphore.release();
            } catch (CancellationException | ExecutionException | InterruptedException e) {
                // 正常完成或已被其他路径处理，忽略
            }
        }, "agent-timeout-" + executionId);
        timeoutThread.setDaemon(true);
        timeoutThread.start();
    }

    private String generateExecutionId() {
        String date = LocalDateTime.now().format(DATE_FMT);
        long seq = taskCounter.incrementAndGet();
        return String.format("exec-%s-%03d", date, seq);
    }
}
