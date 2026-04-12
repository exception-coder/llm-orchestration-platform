package com.exceptioncoder.llm.infrastructure.agent.task;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Agent 异步执行配置。
 *
 * <p>提供线程池、并发信号量、超时参数等核心 Bean 和配置项。</p>
 */
@Data
@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "agent.async")
public class AgentAsyncConfig {

    /** 最大并发执行数 */
    private int maxConcurrent = 5;

    /** 线程池核心/最大大小 */
    private int threadPoolSize = 5;

    /** 默认执行超时（秒），可被 AgentDefinition.timeoutSeconds 覆盖 */
    private int executionTimeout = 120;

    /** 已完成任务保留时长（分钟） */
    private int taskRetainMinutes = 30;

    /** SSE 连接超时（毫秒） */
    private long sseTimeout = 600_000;

    @Bean("agentExecutorPool")
    public ExecutorService agentExecutorPool() {
        return Executors.newFixedThreadPool(threadPoolSize);
    }

    @Bean("agentConcurrencySemaphore")
    public Semaphore agentConcurrencySemaphore() {
        return new Semaphore(maxConcurrent);
    }
}
