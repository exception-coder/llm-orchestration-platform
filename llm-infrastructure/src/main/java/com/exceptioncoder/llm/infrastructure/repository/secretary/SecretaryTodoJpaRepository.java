package com.exceptioncoder.llm.infrastructure.repository.secretary;

import com.exceptioncoder.llm.infrastructure.entity.secretary.SecretaryTodoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecretaryTodoJpaRepository extends JpaRepository<SecretaryTodoEntity, Long> {

    List<SecretaryTodoEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<SecretaryTodoEntity> findByUserIdAndDoneFalseOrderByPriorityDescCreatedAtDesc(String userId);
}
