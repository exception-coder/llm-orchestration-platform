package com.exceptioncoder.llm.domain.repository;

import com.exceptioncoder.llm.domain.model.SecretaryTodo;

import java.util.List;
import java.util.Optional;

/**
 * 待办仓储接口
 */
public interface SecretaryTodoRepository {

    SecretaryTodo save(SecretaryTodo todo);

    Optional<SecretaryTodo> findById(Long id);

    List<SecretaryTodo> findByUserId(String userId);

    List<SecretaryTodo> findPending(String userId);

    void deleteById(Long id);

    SecretaryTodo markDone(Long id);
}
