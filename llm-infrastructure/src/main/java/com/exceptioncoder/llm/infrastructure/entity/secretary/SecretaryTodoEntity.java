package com.exceptioncoder.llm.infrastructure.entity.secretary;

import com.exceptioncoder.llm.domain.model.SecretaryTodo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "secretary_todo", indexes = {
        @Index(name = "idx_secretary_todo_user", columnList = "user_id")
})
public class SecretaryTodoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private SecretaryTodo.Priority priority;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "done", nullable = false)
    private boolean done;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
