package com.exceptioncoder.llm.infrastructure.devplan.repository;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanTask;
import com.exceptioncoder.llm.domain.devplan.repository.DevPlanTaskRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务仓储 — 内存实现（一期先用内存，后续切换 JPA）
 */
@Repository
public class InMemoryDevPlanTaskRepository implements DevPlanTaskRepository {

    private final Map<String, DevPlanTask> store = new ConcurrentHashMap<>();

    @Override
    public DevPlanTask save(DevPlanTask task) {
        store.put(task.taskId(), task);
        return task;
    }

    @Override
    public Optional<DevPlanTask> findByTaskId(String taskId) {
        return Optional.ofNullable(store.get(taskId));
    }

    @Override
    public void updateStatus(String taskId, String status, String currentNode) {
        store.computeIfPresent(taskId, (id, existing) -> DevPlanTask.builder()
                .taskId(existing.taskId())
                .projectPath(existing.projectPath())
                .requirement(existing.requirement())
                .status(status)
                .currentNode(currentNode != null ? currentNode : existing.currentNode())
                .priority(existing.priority())
                .timeoutSeconds(existing.timeoutSeconds())
                .traceId(existing.traceId())
                .startedAt(existing.startedAt())
                .completedAt(existing.completedAt())
                .createdAt(existing.createdAt())
                .build());
    }
}
