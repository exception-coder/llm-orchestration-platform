package com.exceptioncoder.llm.domain.model;

import java.time.LocalDateTime;

/**
 * 秘书日程模型
 */
public record SecretarySchedule(
        Long id,
        String userId,
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean reminder,
        boolean done,
        LocalDateTime createdAt
) {}
