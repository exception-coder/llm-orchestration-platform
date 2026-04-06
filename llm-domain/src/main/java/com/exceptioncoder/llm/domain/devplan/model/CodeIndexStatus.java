package com.exceptioncoder.llm.domain.devplan.model;

import java.time.LocalDateTime;

/**
 * 代码索引状态
 */
public record CodeIndexStatus(
        String projectPath,
        String collectionName,
        int docCount,
        String status,
        LocalDateTime lastIndexedAt,
        String fileHash
) {
    public enum IndexState {
        IDLE, INDEXING, READY, FAILED
    }

    public boolean isReady() {
        return IndexState.READY.name().equals(status);
    }
}
