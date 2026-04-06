package com.exceptioncoder.llm.domain.devplan.repository;

import com.exceptioncoder.llm.domain.devplan.model.CodeIndexStatus;

import java.util.Optional;

/**
 * 索引状态仓储接口 -- 代码索引状态的持久化访问契约。
 *
 * <p>属于 Domain 层 devplan 模块。管理 {@link CodeIndexStatus} 的存取与状态流转，
 * 由 Infrastructure 层提供具体实现。其中 {@link #tryTransitionToIndexing} 采用
 * 乐观锁/CAS 语义，确保同一时刻仅有一个进程对同一项目执行索引操作。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public interface CodeIndexStatusRepository {

    /**
     * 根据项目路径查询索引状态。
     *
     * @param projectPath 目标项目的本地路径
     * @return 索引状态的 Optional 包装，不存在时返回 empty
     */
    Optional<CodeIndexStatus> findByProjectPath(String projectPath);

    /**
     * 保存索引状态（新增或全量更新）。
     *
     * @param status 待保存的索引状态实体
     * @return 保存后的索引状态实体
     */
    CodeIndexStatus save(CodeIndexStatus status);

    /**
     * 尝试将指定项目的索引状态原子性地流转为 INDEXING。
     *
     * <p>仅当当前状态为 IDLE、READY 或 FAILED 时才允许流转，防止并发重复索引。
     * 实现时应使用 CAS 或乐观锁机制保证原子性。</p>
     *
     * @param projectPath 目标项目的本地路径
     * @return 流转成功返回 true，若当前已为 INDEXING 状态则返回 false
     */
    boolean tryTransitionToIndexing(String projectPath);
}
