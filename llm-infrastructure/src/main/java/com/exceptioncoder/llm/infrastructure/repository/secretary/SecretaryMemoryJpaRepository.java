package com.exceptioncoder.llm.infrastructure.repository.secretary;

import com.exceptioncoder.llm.infrastructure.entity.secretary.SecretaryMemoryEntity;
import com.exceptioncoder.llm.domain.model.SecretaryMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecretaryMemoryJpaRepository extends JpaRepository<SecretaryMemoryEntity, Long> {

    List<SecretaryMemoryEntity> findByUserId(String userId);

    List<SecretaryMemoryEntity> findByUserIdAndType(String userId, SecretaryMemory.MemoryType type);

    void deleteByUserId(String userId);
}
