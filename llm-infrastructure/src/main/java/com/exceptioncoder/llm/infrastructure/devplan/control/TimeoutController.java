package com.exceptioncoder.llm.infrastructure.devplan.control;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 开发计划任务超时控制器。
 *
 * <p>属于 Infrastructure层 devplan/control 模块，负责为任务执行施加超时保护，
 * 防止 Agent 调用或长时间运行的计算任务无限期阻塞系统资源。
 *
 * <p><b>技术方案：</b>采用 {@link Future#get(long, TimeUnit)} 实现超时控制，
 * 而非 {@code CompletableFuture.orTimeout}，原因是 Future.get 语义更直观，
 * 且能在超时后通过 {@link Future#cancel(boolean)} 主动中断工作线程。
 *
 * <p><b>线程池选型：</b>使用 {@link Executors#newCachedThreadPool()} 而非固定线程池，
 * 因为开发计划任务的并发数已由 {@link ConcurrencyController} 在上游控制，
 * 此处无需再做线程数限制，CachedThreadPool 的弹性伸缩更适合突发任务场景。
 *
 * <p><b>协作关系：</b>被 {@link DevPlanTaskManagerImpl} 调用，为单个任务的执行过程施加超时边界。
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Component
public class TimeoutController {

    /** 缓存线程池，线程数由上游 ConcurrencyController 间接控制，此处不做额外限制 */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 在指定超时时间内执行任务，超时则取消并抛出异常。
     *
     * @param callable       需要执行的任务
     * @param timeoutSeconds 超时时间（秒）
     * @param <T>            任务返回值类型
     * @return 任务执行结果
     * @throws TaskTimeoutException 任务执行超过指定时间时抛出
     * @throws RuntimeException     任务内部执行异常或线程被中断时抛出
     */
    public <T> T executeWithTimeout(Callable<T> callable, int timeoutSeconds) {
        Future<T> future = executor.submit(callable);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // 超时后主动取消任务，true 表示允许中断正在执行的线程
            future.cancel(true);
            throw new TaskTimeoutException("任务执行超时，timeout=" + timeoutSeconds + "s");
        } catch (ExecutionException e) {
            // 解包 ExecutionException，将真实异常作为 cause 向上传播
            throw new RuntimeException("任务执行异常: " + e.getCause().getMessage(), e.getCause());
        } catch (InterruptedException e) {
            // 恢复中断标志，遵循 Java 并发最佳实践
            Thread.currentThread().interrupt();
            throw new RuntimeException("任务被中断", e);
        }
    }

    /**
     * 任务超时异常，当任务执行时间超过预设阈值时抛出。
     */
    public static class TaskTimeoutException extends RuntimeException {
        /**
         * 构造任务超时异常。
         *
         * @param message 异常描述信息，包含超时时间
         */
        public TaskTimeoutException(String message) {
            super(message);
        }
    }
}
