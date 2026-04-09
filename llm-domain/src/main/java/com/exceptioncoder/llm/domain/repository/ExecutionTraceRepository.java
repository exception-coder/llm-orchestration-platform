package com.exceptioncoder.llm.domain.repository;

import com.exceptioncoder.llm.domain.model.ExecutionTrace;

import java.util.List;
import java.util.Optional;

/**
 * 执行流水仓储接口。
 */
public interface ExecutionTraceRepository {

    ExecutionTrace save(ExecutionTrace trace);

    Optional<ExecutionTrace> findByTraceId(String traceId);

    /**
     * 按 Agent ID 查询最近 N 条执行记录（不含 steps 明细，需按 traceId 单独查）。
     */
    List<ExecutionTrace> findByAgentId(String agentId, int limit);

    /**
     * 查询最近 N 条所有执行记录。
     */
    List<ExecutionTrace> findRecent(int limit);
}
