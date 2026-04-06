package com.exceptioncoder.llm.domain.devplan.repository;

import com.exceptioncoder.llm.domain.devplan.model.CodeIndexStatus;

import java.util.Optional;

/**
 * 索引状态仓储接口
 */
public interface CodeIndexStatusRepository {

    Optional<CodeIndexStatus> findByProjectPath(String projectPath);

    CodeIndexStatus save(CodeIndexStatus status);

    boolean tryTransitionToIndexing(String projectPath);
}
