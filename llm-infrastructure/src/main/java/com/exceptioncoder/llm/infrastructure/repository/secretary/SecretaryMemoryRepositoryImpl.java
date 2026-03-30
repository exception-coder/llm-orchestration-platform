package com.exceptioncoder.llm.infrastructure.repository.secretary;

import com.exceptioncoder.llm.domain.model.SecretaryMemory;
import com.exceptioncoder.llm.domain.repository.SecretaryMemoryRepository;
import com.exceptioncoder.llm.infrastructure.entity.secretary.SecretaryMemoryEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class SecretaryMemoryRepositoryImpl implements SecretaryMemoryRepository {

    private final SecretaryMemoryJpaRepository jpaRepository;

    public SecretaryMemoryRepositoryImpl(SecretaryMemoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SecretaryMemory save(SecretaryMemory memory) {
        return jpaRepository.save(SecretaryMemoryEntity.fromDomain(memory)).toDomain();
    }

    @Override
    public List<SecretaryMemory> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(SecretaryMemoryEntity::toDomain)
                .toList();
    }

    @Override
    public List<SecretaryMemory> findByUserIdAndType(String userId, SecretaryMemory.MemoryType type) {
        return jpaRepository.findByUserIdAndType(userId, type).stream()
                .map(SecretaryMemoryEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByUserId(String userId) {
        jpaRepository.deleteByUserId(userId);
    }
}
