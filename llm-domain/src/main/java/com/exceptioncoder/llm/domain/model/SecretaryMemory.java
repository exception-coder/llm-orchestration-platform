package com.exceptioncoder.llm.domain.model;

import java.time.LocalDateTime;

/**
 * 秘书长期记忆模型
 */
public record SecretaryMemory(
        Long id,
        String userId,
        MemoryType type,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public enum MemoryType {
        /** 用户偏好 */
        PREFERENCE,
        /** 重要事项摘要 */
        SUMMARY,
        /** 用户背景信息 */
        PROFILE
    }
}
