package com.exceptioncoder.llm.infrastructure.devplan.repository;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanTask;
import com.exceptioncoder.llm.domain.devplan.repository.DevPlanTaskRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开发计划任务仓储的内存实现。
 *
 * <p>属于 Infrastructure层 devplan/repository 模块，实现了 Domain 层定义的
 * {@link DevPlanTaskRepository} 接口。采用 {@link ConcurrentHashMap} 作为存储引擎，
 * 提供线程安全的任务 CRUD 操作。
 *
 * <p><b>设计思路：</b>一期使用内存存储快速验证业务流程，降低基础设施依赖；
 * 后续切换为 JPA 实现时，只需新建 {@code JpaDevPlanTaskRepository} 并替换 Bean 注入，
 * 上层代码（面向 {@link DevPlanTaskRepository} 接口编程）无需改动。
 *
 * <p><b>注意事项：</b>内存实现在进程重启后数据丢失，不适合生产环境使用。
 *
 * <p><b>协作关系：</b>
 * <ul>
 *   <li>实现 {@link DevPlanTaskRepository}（Domain 层接口）</li>
 *   <li>被 {@link com.exceptioncoder.llm.infrastructure.devplan.control.DevPlanTaskManagerImpl} 调用</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Repository
public class InMemoryDevPlanTaskRepository implements DevPlanTaskRepository {

    /** 内存存储，key 为 taskId，value 为任务对象；使用 ConcurrentHashMap 保证线程安全 */
    private final Map<String, DevPlanTask> store = new ConcurrentHashMap<>();

    /**
     * 保存任务到内存存储。
     *
     * <p>如果 taskId 已存在则覆盖（等同于更新），不存在则新增。
     *
     * @param task 待保存的任务对象
     * @return 保存后的任务对象（与入参相同）
     */
    @Override
    public DevPlanTask save(DevPlanTask task) {
        store.put(task.taskId(), task);
        return task;
    }

    /**
     * 根据任务 ID 查找任务。
     *
     * @param taskId 任务唯一标识
     * @return 包含任务对象的 Optional，不存在时返回 empty
     */
    @Override
    public Optional<DevPlanTask> findByTaskId(String taskId) {
        return Optional.ofNullable(store.get(taskId));
    }

    /**
     * 更新任务的状态和当前节点。
     *
     * <p>由于 {@link DevPlanTask} 为不可变对象（Record 或 Builder 模式），
     * 更新时需通过 Builder 重建新对象并替换旧对象。
     * 使用 {@link ConcurrentHashMap#computeIfPresent} 保证原子性，
     * 避免并发场景下的读写冲突。
     *
     * @param taskId      任务唯一标识
     * @param status      新的状态值
     * @param currentNode 当前执行节点名称，为 null 时保留原值
     */
    @Override
    public void updateStatus(String taskId, String status, String currentNode) {
        // computeIfPresent 保证只在 key 存在时执行更新，且操作是原子的
        store.computeIfPresent(taskId, (id, existing) -> DevPlanTask.builder()
                .taskId(existing.taskId())
                .projectPath(existing.projectPath())
                .requirement(existing.requirement())
                .status(status)
                // currentNode 为 null 时保留原值，非 null 时更新
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
