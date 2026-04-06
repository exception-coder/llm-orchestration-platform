package com.exceptioncoder.llm.infrastructure.devplan.control;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

/**
 * 并发控制 — 基于 Semaphore 限制同时执行的任务数
 */
@Slf4j
@Component
public class ConcurrencyController {

    private final Semaphore semaphore;

    public ConcurrencyController(@Value("${devplan.max-concurrent:3}") int maxConcurrent) {
        this.semaphore = new Semaphore(maxConcurrent);
        log.info("并发控制初始化，maxConcurrent={}", maxConcurrent);
    }

    public void acquire() {
        if (!semaphore.tryAcquire()) {
            throw new ConcurrencyExceededException("并发超限，请稍后重试");
        }
        log.debug("获取并发槽位，剩余={}", semaphore.availablePermits());
    }

    public void release() {
        semaphore.release();
        log.debug("释放并发槽位，剩余={}", semaphore.availablePermits());
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }

    public static class ConcurrencyExceededException extends RuntimeException {
        public ConcurrencyExceededException(String message) {
            super(message);
        }
    }
}
