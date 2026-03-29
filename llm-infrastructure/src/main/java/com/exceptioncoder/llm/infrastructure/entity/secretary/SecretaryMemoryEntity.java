package com.exceptioncoder.llm.infrastructure.entity.secretary;

import com.exceptioncoder.llm.domain.model.SecretaryMemory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "secretary_memory", indexes = {
        @Index(name = "idx_secretary_memory_user", columnList = "user_id")
})
public class SecretaryMemoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private SecretaryMemory.MemoryType type;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SecretaryMemory toDomain() {
        return new SecretaryMemory(id, userId, type, content, createdAt, updatedAt);
    }

    public static SecretaryMemoryEntity fromDomain(SecretaryMemory memory) {
        return SecretaryMemoryEntity.builder()
                .id(memory.id())
                .userId(memory.userId())
                .type(memory.type())
                .content(memory.content())
                .build();
    }
}
