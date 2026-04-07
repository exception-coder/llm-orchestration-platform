package com.exceptioncoder.llm.infrastructure.devplan.repository;

import com.exceptioncoder.llm.domain.devplan.model.CodeIndexStatus;
import com.exceptioncoder.llm.domain.devplan.repository.CodeIndexStatusRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 代码索引状态仓储的内存实现。
 *
 * <p>一期使用内存存储快速验证，后续切换为 JPA 实现（持久化到 MySQL）。
 * tryTransitionToIndexing 通过 synchronized 保证原子性。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Repository
public class InMemoryCodeIndexStatusRepository implements CodeIndexStatusRepository {

    private final Map<String, CodeIndexStatus> store = new ConcurrentHashMap<>();

    @Override
    public Optional<CodeIndexStatus> findByProjectPath(String projectPath) {
        return Optional.ofNullable(store.get(projectPath));
    }

    @Override
    public CodeIndexStatus save(CodeIndexStatus status) {
        store.put(status.projectPath(), status);
        return status;
    }

    @Override
    public synchronized boolean tryTransitionToIndexing(String projectPath) {
        CodeIndexStatus current = store.get(projectPath);
        if (current != null && CodeIndexStatus.IndexState.INDEXING.name().equals(current.status())) {
            return false;
        }
        return true;
    }
}
