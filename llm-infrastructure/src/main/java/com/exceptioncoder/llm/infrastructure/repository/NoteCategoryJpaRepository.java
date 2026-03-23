package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.infrastructure.entity.NoteCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 记录类目 JPA Repository
 */
@Repository
public interface NoteCategoryJpaRepository extends JpaRepository<NoteCategoryEntity, Long> {

    Optional<NoteCategoryEntity> findByName(String name);

    boolean existsByName(String name);
}
