package com.exceptioncoder.llm.infrastructure.repository.secretary;

import com.exceptioncoder.llm.domain.model.SecretaryTodo;
import com.exceptioncoder.llm.domain.repository.SecretaryTodoRepository;
import com.exceptioncoder.llm.infrastructure.entity.secretary.SecretaryTodoEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SecretaryTodoRepositoryImpl implements SecretaryTodoRepository {

    private final SecretaryTodoJpaRepository jpaRepository;

    public SecretaryTodoRepositoryImpl(SecretaryTodoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SecretaryTodo save(SecretaryTodo todo) {
        SecretaryTodoEntity entity = SecretaryTodoEntity.builder()
                .id(todo.id())
                .userId(todo.userId())
                .title(todo.title())
                .priority(todo.priority())
                .dueDate(todo.dueDate())
                .done(todo.done())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<SecretaryTodo> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<SecretaryTodo> findByUserId(String userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SecretaryTodo> findPending(String userId) {
        return jpaRepository.findByUserIdAndDoneFalseOrderByPriorityDescCreatedAtDesc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public SecretaryTodo markDone(Long id) {
        SecretaryTodoEntity entity = jpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("待办不存在: " + id));
        entity.setDone(true);
        return toDomain(jpaRepository.save(entity));
    }

    private SecretaryTodo toDomain(SecretaryTodoEntity e) {
        return new SecretaryTodo(
                e.getId(), e.getUserId(), e.getTitle(), e.getPriority(),
                e.getDueDate(), e.isDone(), e.getCreatedAt());
    }
}
