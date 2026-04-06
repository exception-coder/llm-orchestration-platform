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
 * 超时控制 — 基于 Future.get(timeout) 实现任务级超时
 */
@Slf4j
@Component
public class TimeoutController {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public <T> T executeWithTimeout(Callable<T> callable, int timeoutSeconds) {
        Future<T> future = executor.submit(callable);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new TaskTimeoutException("任务执行超时，timeout=" + timeoutSeconds + "s");
        } catch (ExecutionException e) {
            throw new RuntimeException("任务执行异常: " + e.getCause().getMessage(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("任务被中断", e);
        }
    }

    public static class TaskTimeoutException extends RuntimeException {
        public TaskTimeoutException(String message) {
            super(message);
        }
    }
}
