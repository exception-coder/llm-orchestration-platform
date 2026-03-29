package com.exceptioncoder.llm.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 秘书待办模型
 */
public record SecretaryTodo(
        Long id,
        String userId,
        String title,
        Priority priority,
        LocalDate dueDate,
        boolean done,
        LocalDateTime createdAt
) {
    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }
}
