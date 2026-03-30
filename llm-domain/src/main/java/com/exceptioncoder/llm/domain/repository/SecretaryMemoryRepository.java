package com.exceptioncoder.llm.domain.repository;

import com.exceptioncoder.llm.domain.model.SecretaryMemory;

import java.util.List;

/**
 * 秘书记忆仓储接口
 */
public interface SecretaryMemoryRepository {

    SecretaryMemory save(SecretaryMemory memory);

    List<SecretaryMemory> findByUserId(String userId);

    List<SecretaryMemory> findByUserIdAndType(String userId, SecretaryMemory.MemoryType type);

    void deleteByUserId(String userId);
}
