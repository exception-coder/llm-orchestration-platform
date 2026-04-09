package com.exceptioncoder.llm.domain.devplan.model;

import java.time.LocalDateTime;

/**
 * 项目画像索引状态 -- 记录 ProjectProfile 各维度在向量库中的索引情况。
 *
 * <p>与 {@link CodeIndexStatus} 分离管理，因为画像索引的粒度是"项目×维度"，
 * 而代码索引的粒度是"项目×文件"。通过 projectPath + dimension 唯一定位一条记录。
 *
 * @author zhangkai
 * @since 2026-04-08
 */
public record ProfileIndexStatus(
        String projectPath,
        String projectName,
        ProfileDimension dimension,
        String status,
        LocalDateTime lastIndexedAt,
        String contentHash
) {

    public enum IndexState {
        IDLE, INDEXING, READY, FAILED
    }

    public boolean isReady() {
        return IndexState.READY.name().equals(status);
    }

    /**
     * 构建存储 key：projectPath + dimension 唯一标识。
     */
    public String storeKey() {
        return projectPath + "::" + dimension.name();
    }
}
