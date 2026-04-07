package com.exceptioncoder.llm.domain.devplan.repository;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanTask;

import java.util.Optional;

/**
 * 任务仓储接口 -- 开发方案任务的持久化访问契约。
 *
 * <p>属于 Domain 层 devplan 模块。定义任务实体的 CRUD 操作，由 Infrastructure 层
 * 提供具体的数据库实现。遵循仓储模式，隔离领域层与数据访问技术细节。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public interface DevPlanTaskRepository {

    /**
     * 保存任务实体（新增或全量更新）。
     *
     * @param task 待保存的任务实体
     * @return 保存后的任务实体（可能包含数据库生成的字段）
     */
    DevPlanTask save(DevPlanTask task);

    /**
     * 根据任务 ID 查询任务实体。
     *
     * @param taskId 任务唯一标识
     * @return 任务实体的 Optional 包装，不存在时返回 empty
     */
    Optional<DevPlanTask> findByTaskId(String taskId);

    /**
     * 更新指定任务的状态与当前节点。
     *
     * @param taskId      任务唯一标识
     * @param status      目标状态
     * @param currentNode 当前正在执行的 Graph 节点名称
     */
    void updateStatus(String taskId, String status, String currentNode);
}
