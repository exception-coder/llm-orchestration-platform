package com.exceptioncoder.llm.infrastructure.devplan.control;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

/**
 * 开发计划任务并发控制器。
 *
 * <p>属于 Infrastructure层 devplan/control 模块，负责对同时执行的开发计划任务数量进行限流保护。
 * 采用 {@link Semaphore} 而非阻塞队列的原因：
 * <ul>
 *   <li>Semaphore 提供非阻塞的 tryAcquire 语义，超限时可立即快速失败并返回友好提示，
 *       而阻塞队列会让调用方挂起等待，不适合面向用户的 HTTP 请求场景</li>
 *   <li>Semaphore 天然支持公平/非公平策略切换，且与业务解耦，不需要维护额外的任务队列数据结构</li>
 * </ul>
 *
 * <p><b>协作关系：</b>被 {@link DevPlanTaskManagerImpl} 在任务提交时调用 acquire 获取槽位，
 * 在任务完成或失败时调用 release 归还槽位。
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Component
public class ConcurrencyController {

    /** 信号量，用于控制最大并发任务数；permit 数量通过配置项 devplan.max-concurrent 注入，默认 3 */
    private final Semaphore semaphore;

    /**
     * 构造并发控制器并初始化信号量。
     *
     * @param maxConcurrent 允许同时执行的最大任务数，来源于配置项 {@code devplan.max-concurrent}，默认值为 3
     */
    public ConcurrencyController(@Value("${devplan.max-concurrent:3}") int maxConcurrent) {
        this.semaphore = new Semaphore(maxConcurrent);
        log.info("并发控制初始化，maxConcurrent={}", maxConcurrent);
    }

    /**
     * 尝试获取一个并发执行槽位。
     *
     * <p>使用 {@code tryAcquire} 而非 {@code acquire} 的原因：tryAcquire 是非阻塞的，
     * 当所有槽位已被占用时会立即返回 false，从而可以快速向调用方抛出超限异常；
     * 而 acquire 会无限阻塞当前线程直到有槽位释放，在 HTTP 请求链路中可能导致线程池耗尽。
     *
     * @throws ConcurrencyExceededException 当前并发数已达上限时抛出
     */
    public void acquire() {
        // 非阻塞尝试获取，失败则快速拒绝
        if (!semaphore.tryAcquire()) {
            throw new ConcurrencyExceededException("并发超限，请稍后重试");
        }
        log.debug("获取并发槽位，剩余={}", semaphore.availablePermits());
    }

    /**
     * 释放一个并发执行槽位。
     *
     * <p>必须在任务完成（成功或失败）后调用，否则会导致槽位泄漏，最终所有新任务都被拒绝。
     */
    public void release() {
        semaphore.release();
        log.debug("释放并发槽位，剩余={}", semaphore.availablePermits());
    }

    /**
     * 查询当前可用的并发槽位数量。
     *
     * @return 剩余可用槽位数
     */
    public int availablePermits() {
        return semaphore.availablePermits();
    }

    /**
     * 并发超限异常，当任务提交时所有并发槽位均已被占用时抛出。
     */
    public static class ConcurrencyExceededException extends RuntimeException {
        /**
         * 构造并发超限异常。
         *
         * @param message 异常描述信息
         */
        public ConcurrencyExceededException(String message) {
            super(message);
        }
    }
}
