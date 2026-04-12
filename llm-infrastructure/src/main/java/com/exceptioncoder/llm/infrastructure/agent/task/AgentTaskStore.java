package com.exceptioncoder.llm.infrastructure.agent.task;

import com.exceptioncoder.llm.domain.model.AgentTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * Agent 任务内存存储。
 *
 * <p>基于 ConcurrentHashMap 实现，提供线程安全的任务 CRUD 和定时过期清理。
 * 一期方案，后续可替换为 Redis 实现以支持多实例共享。</p>
 */
@Slf4j
@Component
public class AgentTaskStore {

    private final ConcurrentHashMap<String, AgentTask> store = new ConcurrentHashMap<>();
    private final AgentAsyncConfig config;

    public AgentTaskStore(AgentAsyncConfig config) {
        this.config = config;
    }

    public void save(AgentTask task) {
        store.put(task.executionId(), task);
    }

    public Optional<AgentTask> get(String executionId) {
        return Optional.ofNullable(store.get(executionId));
    }

    /**
     * 原子更新任务状态。
     */
    public void update(String executionId, UnaryOperator<AgentTask> updater) {
        store.computeIfPresent(executionId, (k, existing) -> updater.apply(existing));
    }

    /**
     * 定时清理已完成且超过保留时长的任务。
     */
    @Scheduled(fixedRate = 60_000)
    public void cleanExpired() {
        Duration retain = Duration.ofMinutes(config.getTaskRetainMinutes());
        LocalDateTime cutoff = LocalDateTime.now().minus(retain);
        int before = store.size();

        store.entrySet().removeIf(entry -> {
            AgentTask task = entry.getValue();
            boolean terminal = task.status() == AgentTask.Status.COMPLETED
                    || task.status() == AgentTask.Status.FAILED
                    || task.status() == AgentTask.Status.TIMED_OUT;
            return terminal && task.completedAt() != null && task.completedAt().isBefore(cutoff);
        });

        int removed = before - store.size();
        if (removed > 0) {
            log.info("清理过期 Agent 任务: removed={}, remaining={}", removed, store.size());
        }
    }
}
